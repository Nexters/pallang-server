package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.group.application.GroupMemberPreview;
import com.nexters.palang.domain.group.application.GroupSummaryProjection;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberStatus;
import com.nexters.palang.domain.group.domain.GroupReadingSummary;
import com.nexters.palang.domain.group.domain.GroupStatus;
import com.nexters.palang.domain.group.domain.QGroup;
import com.nexters.palang.domain.group.domain.QGroupBookEntry;
import com.nexters.palang.domain.group.domain.QGroupMember;
import com.nexters.palang.domain.group.domain.ReadingStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.ArrayList;
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

    // 모임 카드에 보여줄 아바타 수. 나머지는 "+N"으로 표시한다(memberCount - 미리보기 수).
    public static final int MEMBER_PREVIEW_LIMIT = 5;

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

        Map<Long, List<GroupBookEntry>> entriesByGroupId = findEntries(groupIds);
        Map<Long, List<GroupMemberPreview>> previewsByGroupId = findMemberPreviews(groupIds);

        List<GroupSummaryProjection> content = groupIds.stream()
                .filter(groupId -> groupsById.containsKey(groupId) && entriesByGroupId.containsKey(groupId))
                // host는 fetch join 대상이 아니라 LAZY 프록시지만, id만 읽는 건 프록시 초기화 없이도
                // 안전하다(식별자는 프록시 생성 시점에 이미 채워져 있음) — LazyInitializationException 걱정 없다.
                .map(groupId -> toProjection(
                        groupsById.get(groupId), entriesByGroupId.get(groupId),
                        memberCountsByGroupId.getOrDefault(groupId, 0L),
                        previewsByGroupId.getOrDefault(groupId, List.of()), userId, today))
                .toList();

        long total = countMyGroups(userId);
        return new PageImpl<>(content, pageable, total);
    }

    // 모임별 entry 전체(등록 순, id 오름차순). 첫 항목이 기존 단일 책 응답 호환용 대표 책이다(GroupBookEntries 참고).
    private Map<Long, List<GroupBookEntry>> findEntries(List<Long> groupIds) {
        QGroupBookEntry entry = QGroupBookEntry.groupBookEntry;
        Map<Long, List<GroupBookEntry>> entriesByGroupId = new LinkedHashMap<>();
        queryFactory
                .selectFrom(entry)
                .join(entry.book).fetchJoin()
                .where(entry.group.id.in(groupIds))
                .orderBy(entry.id.asc())
                .fetch()
                .forEach(e -> entriesByGroupId.computeIfAbsent(e.getGroup().getId(), id -> new ArrayList<>()).add(e));
        return entriesByGroupId;
    }

    // 모임별 카드 아바타: 현재 멤버를 모임장 → 참여일 → userId 순으로 최대 MEMBER_PREVIEW_LIMIT명. 멤버십은
    // (모임, 사용자)당 한 행이라 같은 사람이 두 번 나오지 않는다. 정원이 최대 10명이라 모임별로 전부 읽은 뒤 자른다.
    private Map<Long, List<GroupMemberPreview>> findMemberPreviews(List<Long> groupIds) {
        QGroupMember groupMember = QGroupMember.groupMember;
        Map<Long, List<GroupMemberPreview>> previewsByGroupId = new LinkedHashMap<>();
        queryFactory
                .selectFrom(groupMember)
                .join(groupMember.user).fetchJoin()
                .where(groupMember.group.id.in(groupIds), groupMember.status.eq(GroupMemberStatus.ACTIVE))
                .orderBy(groupMember.role.asc(), groupMember.joinedAt.asc(), groupMember.user.id.asc())
                .fetch()
                .forEach(member -> {
                    List<GroupMemberPreview> previews =
                            previewsByGroupId.computeIfAbsent(member.getGroup().getId(), id -> new ArrayList<>());
                    if (previews.size() < MEMBER_PREVIEW_LIMIT) {
                        previews.add(new GroupMemberPreview(member.getUser().getId(), member.getUser().getNickname(),
                                member.getUser().getProfileImageUrl()));
                    }
                });
        return previewsByGroupId;
    }

    private GroupSummaryProjection toProjection(Group group, List<GroupBookEntry> entries, long memberCount,
            List<GroupMemberPreview> memberPreviews, Long userId, LocalDate today) {
        GroupBookEntry primaryEntry = entries.getFirst();
        return new GroupSummaryProjection(
                group.getId(), group.getName(), primaryEntry.getBook().getId(), primaryEntry.getBook().getTitle(),
                primaryEntry.getBook().getCoverImageUrl(), memberCount, group.getCapacity(),
                primaryEntry.getStartDate(), primaryEntry.getEndDate(),
                primaryEntry.currentStatus(today) == ReadingStatus.FINISHED, group.getHost().getId().equals(userId),
                GroupReadingSummary.of(entries, today), memberPreviews);
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
