package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.group.application.GroupSummaryProjection;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.QGroup;
import com.nexters.palang.domain.group.domain.QGroupMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GroupQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 내가 속한 모임 목록(최근 생성 순). Group당 memberCount를 fetch join 한 번으로 같이 뽑을 수 없어
    // (group by가 페이지 대상 group들을 벗어난 집계를 왜곡시킴) id 목록을 먼저 페이지네이션한 뒤
    // 그 id들에 대해서만 멤버 수를 별도로 집계한다.
    public Page<GroupSummaryProjection> findMyGroups(Long userId, Pageable pageable) {
        QGroup group = QGroup.group;
        QGroupMember groupMember = QGroupMember.groupMember;

        List<Long> groupIds = queryFactory
                .select(group.id)
                .from(group)
                .join(groupMember).on(groupMember.group.eq(group))
                .where(groupMember.user.id.eq(userId))
                .orderBy(group.createdAt.desc(), group.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 마지막 페이지를 넘어가는 page를 요청하면 groupIds는 비지만, totalElements는 여전히 실제
        // 전체 건수를 내려줘야 pageInfo(totalPages/hasNext)가 올바르다.
        if (groupIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, countMyGroups(userId));
        }

        Map<Long, Long> memberCountsByGroupId = queryFactory
                .select(groupMember.group.id, groupMember.count())
                .from(groupMember)
                .where(groupMember.group.id.in(groupIds))
                .groupBy(groupMember.group.id)
                .fetch().stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(groupMember.group.id),
                        tuple -> tuple.get(groupMember.count())));

        List<Group> groups = queryFactory
                .selectFrom(group)
                .join(group.book).fetchJoin()
                .where(group.id.in(groupIds))
                .fetch();
        Map<Long, Group> groupsById = groups.stream().collect(Collectors.toMap(Group::getId, g -> g));

        List<GroupSummaryProjection> content = groupIds.stream()
                .map(groupsById::get)
                .filter(Objects::nonNull)
                .map(g -> new GroupSummaryProjection(
                        g.getId(), g.getName(), g.getBook().getId(), g.getBook().getTitle(),
                        g.getBook().getCoverImageUrl(), memberCountsByGroupId.getOrDefault(g.getId(), 0L),
                        g.getCapacity(), g.getStartDate(), g.getEndDate()))
                .toList();

        long total = countMyGroups(userId);
        return new PageImpl<>(content, pageable, total);
    }

    private long countMyGroups(Long userId) {
        QGroup group = QGroup.group;
        QGroupMember groupMember = QGroupMember.groupMember;

        Long total = queryFactory
                .select(group.count())
                .from(group)
                .join(groupMember).on(groupMember.group.eq(group))
                .where(groupMember.user.id.eq(userId))
                .fetchOne();
        return total != null ? total : 0L;
    }

    // 모임 멤버 목록: 모임장(HOST)이 먼저, 그다음 가입 순.
    public Page<GroupMember> findMembers(Long groupId, Pageable pageable) {
        QGroupMember groupMember = QGroupMember.groupMember;

        List<GroupMember> content = queryFactory
                .selectFrom(groupMember)
                .join(groupMember.user).fetchJoin()
                .where(groupMember.group.id.eq(groupId))
                .orderBy(groupMember.role.asc(), groupMember.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(groupMember.count())
                .from(groupMember)
                .where(groupMember.group.id.eq(groupId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
