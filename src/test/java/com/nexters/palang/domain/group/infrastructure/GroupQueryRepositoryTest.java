package com.nexters.palang.domain.group.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.group.application.GroupSummaryProjection;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberRole;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class GroupQueryRepositoryTest {

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
        Group built = Group.create(name, book, host, 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));
        return entityManager.persistAndFlush(built);
    }

    private void join(Group group, User user, GroupMemberRole role) {
        entityManager.persistAndFlush(GroupMember.of(group, user, role));
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

        Pageable pageable = PageRequest.of(0, 20);
        Page<GroupSummaryProjection> result = groupQueryRepository.findMyGroups(me.getId(), pageable);

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
    @DisplayName("마지막 페이지를 넘어가는 페이지를 요청해도 전체 건수는 정확히 반환한다")
    void findMyGroupsReturnsTotalElementsEvenWhenPageIsEmpty() {
        User me = user("me");
        join(group("고전 뽀개기", book("채식주의자"), me), me, GroupMemberRole.HOST);
        join(group("주말 독서 모임", book("소년이 온다"), me), me, GroupMemberRole.HOST);

        // 전체 2건인데 첫 페이지(size=20)를 넘어가는 두 번째 페이지를 요청 -> content는 비지만 total은 2여야 한다.
        Page<GroupSummaryProjection> result = groupQueryRepository.findMyGroups(me.getId(), PageRequest.of(1, 20));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("모임 멤버 목록은 모임장이 먼저, 그다음 가입 순으로 조회한다")
    void findMembers() {
        User host = user("host");
        User member = user("member");
        Group group = group("고전 뽀개기", book("채식주의자"), host);
        join(group, host, GroupMemberRole.HOST);
        join(group, member, GroupMemberRole.MEMBER);

        Page<GroupMember> result = groupQueryRepository.findMembers(group.getId(), PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(m -> m.getUser().getId())
                .containsExactly(host.getId(), member.getId());
    }
}
