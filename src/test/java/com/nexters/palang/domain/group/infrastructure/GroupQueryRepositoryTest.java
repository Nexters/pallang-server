package com.nexters.palang.domain.group.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.group.application.GroupSummaryProjection;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberRole;
import com.nexters.palang.domain.group.domain.GroupMemberStatus;
import com.nexters.palang.domain.group.domain.GroupStatus;
import com.nexters.palang.domain.group.domain.ReadingPeriod;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class GroupQueryRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);
    private static final LocalDateTime JOINED_AT = LocalDateTime.of(2026, 8, 1, 12, 0);

    @Autowired
    private TestEntityManager entityManager;

    private GroupQueryRepository groupQueryRepository;

    @BeforeEach
    void setUp() {
        groupQueryRepository = new GroupQueryRepository(new JPAQueryFactory(entityManager.getEntityManager()));
    }

    private User user(String snsId) {
        return entityManager.persistAndFlush(User.builder()
                .nickname("닉네임" + snsId)
                .snsProvider(SnsProvider.KAKAO)
                .snsId(snsId)
                .build());
    }

    private Book book(String title) {
        return entityManager.persistAndFlush(Book.builder()
                .title(title).author("한강").publisher("창비").pageCount(268).source(BookSource.API).build());
    }

    private Group group(String name, Book book, User host) {
        return group(name, book, host, ReadingPeriod.of(TODAY, TODAY.plusDays(30)));
    }

    private Group group(String name, Book book, User host, ReadingPeriod period) {
        Group built = entityManager.persistAndFlush(Group.create(name, host, 4));
        entityManager.persistAndFlush(GroupBookEntry.create(built, book, period, TODAY));
        return built;
    }

    private GroupMember join(Group group, User user, GroupMemberRole role) {
        return join(group, user, role, JOINED_AT);
    }

    private GroupMember join(Group group, User user, GroupMemberRole role, LocalDateTime joinedAt) {
        return entityManager.persistAndFlush(GroupMember.join(group, user, role, joinedAt));
    }

    @Test
    @DisplayName("내가 속한 모임만 최근 생성 순으로, 참여 인원 수와 함께 조회한다")
    void findMyGroups() {
        User me = user("me");
        User other = user("other");
        Book book1 = book("채식주의자");
        Book book2 = book("소년이 온다");

        Group myGroup1 = group("고전 뽀개기", book1, me);
        join(myGroup1, me, GroupMemberRole.HOST);

        Group myGroup2 = group("주말 독서 모임", book2, other);
        join(myGroup2, other, GroupMemberRole.HOST);
        join(myGroup2, me, GroupMemberRole.MEMBER);

        Group notMyGroup = group("남의 모임", book1, other);
        join(notMyGroup, other, GroupMemberRole.HOST);

        Page<GroupSummaryProjection> result = groupQueryRepository.findMyGroups(me.getId(), TODAY, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(GroupSummaryProjection::groupId)
                .containsExactly(myGroup2.getId(), myGroup1.getId());
        assertThat(result.getContent().get(0).memberCount()).isEqualTo(2L);
        assertThat(result.getContent().get(1).memberCount()).isEqualTo(1L);
        assertThat(result.getContent().get(0).isHost()).isFalse(); // myGroup2는 other가 host, me는 member
        assertThat(result.getContent().get(1).isHost()).isTrue(); // myGroup1은 me가 host
    }

    @Test
    @DisplayName("카드의 책과 기간은 모임에 가장 먼저 등록된 책 기준이다")
    void findMyGroupsUsesFirstRegisteredBook() {
        User me = user("me");
        Group group = group("고전 뽀개기", book("채식주의자"), me, ReadingPeriod.of(TODAY, TODAY.plusDays(7)));
        entityManager.persistAndFlush(GroupBookEntry.create(group, book("소년이 온다"), ReadingPeriod.NONE, TODAY));
        join(group, me, GroupMemberRole.HOST);

        GroupSummaryProjection card = groupQueryRepository.findMyGroups(me.getId(), TODAY, PageRequest.of(0, 20))
                .getContent().getFirst();

        assertThat(card.bookTitle()).isEqualTo("채식주의자");
        assertThat(card.startDate()).isEqualTo(TODAY);
        assertThat(card.endDate()).isEqualTo(TODAY.plusDays(7));
    }

    @Test
    @DisplayName("대표 책의 종료일이 지났으면 ended가 true이고, 기간이 없으면 날짜는 null, ended는 false다")
    void findMyGroupsCalculatesEndedFromPrimaryBook() {
        User me = user("me");
        Group noPeriod = group("기간 없음", book("채식주의자"), me, ReadingPeriod.NONE);
        join(noPeriod, me, GroupMemberRole.HOST);
        Group finished = group("끝난 모임", book("소년이 온다"), me, ReadingPeriod.of(TODAY, TODAY));
        join(finished, me, GroupMemberRole.HOST);

        var cards = groupQueryRepository.findMyGroups(me.getId(), TODAY.plusDays(1), PageRequest.of(0, 20)).getContent();

        GroupSummaryProjection finishedCard = cards.get(0);
        GroupSummaryProjection noPeriodCard = cards.get(1);
        assertThat(finishedCard.ended()).isTrue();
        assertThat(noPeriodCard.startDate()).isNull();
        assertThat(noPeriodCard.endDate()).isNull();
        assertThat(noPeriodCard.ended()).isFalse();
    }

    @Test
    @DisplayName("합쳐진(MERGED) 모임과 내가 더 이상 멤버가 아닌 모임은 목록에서 빠지고 인원 수에서도 제외된다")
    void findMyGroupsExcludesMergedGroupsAndInactiveMemberships() {
        User me = user("me");
        User other = user("other");

        Group active = group("남은 모임", book("채식주의자"), me);
        join(active, me, GroupMemberRole.HOST);
        GroupMember removedOther = join(active, other, GroupMemberRole.MEMBER);
        ReflectionTestUtils.setField(removedOther, "status", GroupMemberStatus.REMOVED);
        entityManager.persistAndFlush(removedOther);

        Group merged = group("합쳐진 모임", book("소년이 온다"), me);
        join(merged, me, GroupMemberRole.HOST);
        ReflectionTestUtils.setField(merged, "status", GroupStatus.MERGED);
        entityManager.persistAndFlush(merged);

        Group left = group("나간 모임", book("흰"), other);
        join(left, other, GroupMemberRole.HOST);
        GroupMember myLeftMembership = join(left, me, GroupMemberRole.MEMBER);
        ReflectionTestUtils.setField(myLeftMembership, "status", GroupMemberStatus.LEFT);
        entityManager.persistAndFlush(myLeftMembership);

        Page<GroupSummaryProjection> result = groupQueryRepository.findMyGroups(me.getId(), TODAY, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(GroupSummaryProjection::groupId).containsExactly(active.getId());
        assertThat(result.getContent().getFirst().memberCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("마지막 페이지를 넘어가는 페이지를 요청해도 전체 건수는 정확히 반환한다")
    void findMyGroupsReturnsTotalElementsEvenWhenPageIsEmpty() {
        User me = user("me");
        join(group("고전 뽀개기", book("채식주의자"), me), me, GroupMemberRole.HOST);
        join(group("주말 독서 모임", book("소년이 온다"), me), me, GroupMemberRole.HOST);

        // 전체 2건인데 첫 페이지(size=20)를 넘어가는 두 번째 페이지를 요청 -> content는 비지만 total은 2여야 한다.
        Page<GroupSummaryProjection> result = groupQueryRepository.findMyGroups(me.getId(), TODAY, PageRequest.of(1, 20));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("모임 멤버 목록은 현재 멤버만, 모임장이 먼저, 그다음 참여일 순으로 조회한다")
    void findMembers() {
        User host = user("host");
        User earlyMember = user("early");
        User lateMember = user("late");
        User removedMember = user("removed");
        Group group = group("고전 뽀개기", book("채식주의자"), host);
        join(group, lateMember, GroupMemberRole.MEMBER, JOINED_AT.plusDays(2));
        join(group, host, GroupMemberRole.HOST, JOINED_AT);
        join(group, earlyMember, GroupMemberRole.MEMBER, JOINED_AT.plusDays(1));
        GroupMember removed = join(group, removedMember, GroupMemberRole.MEMBER, JOINED_AT);
        ReflectionTestUtils.setField(removed, "status", GroupMemberStatus.REMOVED);
        entityManager.persistAndFlush(removed);

        Page<GroupMember> result = groupQueryRepository.findMembers(group.getId(), PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
                .extracting(m -> m.getUser().getId())
                .containsExactly(host.getId(), earlyMember.getId(), lateMember.getId());
    }
}
