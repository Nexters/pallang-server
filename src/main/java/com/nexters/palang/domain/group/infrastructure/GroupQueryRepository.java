package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.group.application.GroupSummaryProjection;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberStatus;
import com.nexters.palang.domain.group.domain.GroupStatus;
import com.nexters.palang.domain.group.domain.QGroup;
import com.nexters.palang.domain.group.domain.QGroupBookEntry;
import com.nexters.palang.domain.group.domain.QGroupMember;
import com.nexters.palang.domain.group.domain.ReadingStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    // 내가 현재 가입 완료(ACTIVE)한 ACTIVE 모임 목록(최근 생성 순). 합쳐진 source 모임과 탈퇴/내보내진 모임은
    // 제외한다. Group당 memberCount를 fetch join 한 번으로 같이 뽑을 수 없어(group by가 페이지 대상 group들을
    // 벗어난 집계를 왜곡시킴) id 목록을 먼저 페이지네이션한 뒤 그 id들에 대해서만 멤버 수/대표 책을 따로 읽는다.
    public Page<GroupSummaryProjection> findMyGroups(Long userId, LocalDate today, Pageable pageable) {
        QGroup group = QGroup.group;
        QGroupMember groupMember = QGroupMember.groupMember;

        List<Long> groupIds = queryFactory
                .select(group.id)
                .from(group)
                .join(groupMember).on(groupMember.group.eq(group))
                .where(myActiveGroups(group, groupMember, userId))
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
                .where(groupMember.group.id.in(groupIds), groupMember.status.eq(GroupMemberStatus.ACTIVE))
                .groupBy(groupMember.group.id)
                .fetch().stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(groupMember.group.id),
                        tuple -> tuple.get(groupMember.count())));

        Map<Long, Group> groupsById = queryFactory
                .selectFrom(group)
                .where(group.id.in(groupIds))
                .fetch().stream()
                .collect(Collectors.toMap(Group::getId, g -> g));

        Map<Long, GroupBookEntry> primaryEntriesByGroupId = findPrimaryEntries(groupIds);

        List<GroupSummaryProjection> content = groupIds.stream()
                .filter(groupId -> groupsById.containsKey(groupId) && primaryEntriesByGroupId.containsKey(groupId))
                // host는 fetch join 대상이 아니라 LAZY 프록시지만, id만 읽는 건 프록시 초기화 없이도
                // 안전하다(식별자는 프록시 생성 시점에 이미 채워져 있음) — LazyInitializationException 걱정 없다.
                .map(groupId -> toProjection(
                        groupsById.get(groupId), primaryEntriesByGroupId.get(groupId),
                        memberCountsByGroupId.getOrDefault(groupId, 0L), userId, today))
                .toList();

        long total = countMyGroups(userId);
        return new PageImpl<>(content, pageable, total);
    }

    // 기존 단일 책 응답 호환: 모임별로 가장 먼저 등록된 entry(id 최소)를 대표 책으로 쓴다(GroupBookEntries 참고).
    private Map<Long, GroupBookEntry> findPrimaryEntries(List<Long> groupIds) {
        QGroupBookEntry entry = QGroupBookEntry.groupBookEntry;
        Map<Long, GroupBookEntry> primaryByGroupId = new LinkedHashMap<>();
        queryFactory
                .selectFrom(entry)
                .join(entry.book).fetchJoin()
                .where(entry.group.id.in(groupIds))
                .orderBy(entry.id.asc())
                .fetch()
                .forEach(e -> primaryByGroupId.putIfAbsent(e.getGroup().getId(), e));
        return primaryByGroupId;
    }

    private GroupSummaryProjection toProjection(
            Group group, GroupBookEntry primaryEntry, long memberCount, Long userId, LocalDate today) {
        return new GroupSummaryProjection(
                group.getId(), group.getName(), primaryEntry.getBook().getId(), primaryEntry.getBook().getTitle(),
                primaryEntry.getBook().getCoverImageUrl(), memberCount, group.getCapacity(),
                primaryEntry.getStartDate(), primaryEntry.getEndDate(),
                primaryEntry.currentStatus(today) == ReadingStatus.FINISHED, group.getHost().getId().equals(userId));
    }

    private long countMyGroups(Long userId) {
        QGroup group = QGroup.group;
        QGroupMember groupMember = QGroupMember.groupMember;

        Long total = queryFactory
                .select(group.count())
                .from(group)
                .join(groupMember).on(groupMember.group.eq(group))
                .where(myActiveGroups(group, groupMember, userId))
                .fetchOne();
        return total != null ? total : 0L;
    }

    private BooleanExpression myActiveGroups(QGroup group, QGroupMember groupMember, Long userId) {
        return groupMember.user.id.eq(userId)
                .and(groupMember.status.eq(GroupMemberStatus.ACTIVE))
                .and(group.status.eq(GroupStatus.ACTIVE));
    }

    // 모임 멤버 목록: 현재(ACTIVE) 멤버만. 모임장(HOST)이 먼저, 그다음 참여일 순, 같으면 userId 순.
    public Page<GroupMember> findMembers(Long groupId, Pageable pageable) {
        QGroupMember groupMember = QGroupMember.groupMember;

        List<GroupMember> content = queryFactory
                .selectFrom(groupMember)
                .join(groupMember.user).fetchJoin()
                .where(groupMember.group.id.eq(groupId), groupMember.status.eq(GroupMemberStatus.ACTIVE))
                .orderBy(groupMember.role.asc(), groupMember.joinedAt.asc(), groupMember.user.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(groupMember.count())
                .from(groupMember)
                .where(groupMember.group.id.eq(groupId), groupMember.status.eq(GroupMemberStatus.ACTIVE))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
