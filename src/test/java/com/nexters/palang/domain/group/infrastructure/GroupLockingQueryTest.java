package com.nexters.palang.domain.group.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.GroupInvitation;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberRole;
import com.nexters.palang.domain.group.domain.ReadingPeriod;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

// 모임 행 잠금 뒤에 쓰는 잠금 읽기 쿼리들이 올바른 행을 돌려주는지 확인한다. 잠금 읽기 자체의 동시성 효과(MySQL
// REPEATABLE READ에서 최신 커밋 상태를 읽는 것)는 H2로 재현되지 않아, 여기서는 조회 결과만 검증한다.
@DataJpaTest
@Import(JpaAuditingConfig.class)
class GroupLockingQueryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 26, 12, 0);

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    @Autowired
    private GroupInvitationRepository groupInvitationRepository;
    @Autowired
    private GroupBookEntryRepository groupBookEntryRepository;

    private User host;
    private User member;
    private User removed;
    private Group group;

    @BeforeEach
    void setUp() {
        host = user("host");
        member = user("member");
        removed = user("removed");
        group = entityManager.persistAndFlush(Group.create("고전 뽀개기", host, 4));
        entityManager.persistAndFlush(GroupMember.join(group, host, GroupMemberRole.HOST, NOW));
        entityManager.persistAndFlush(GroupMember.join(group, member, GroupMemberRole.MEMBER, NOW));
        GroupMember removedMembership = GroupMember.join(group, removed, GroupMemberRole.MEMBER, NOW);
        removedMembership.remove(NOW.plusDays(1));
        entityManager.persistAndFlush(removedMembership);
        entityManager.clear();
    }

    private User user(String snsId) {
        return entityManager.persistAndFlush(User.builder()
                .nickname("닉네임" + snsId).snsProvider(SnsProvider.KAKAO).snsId(snsId).build());
    }

    private Book book(String title) {
        return entityManager.persistAndFlush(Book.builder()
                .title(title).author("한강").publisher("창비").pageCount(268).source(BookSource.API).build());
    }

    @Test
    @DisplayName("정원 확인용 잠금 조회는 현재(ACTIVE) 멤버만 돌려준다")
    void findActiveMembersForUpdate() {
        assertThat(groupMemberRepository.findActiveMembersForUpdate(group.getId()))
                .extracting(m -> m.getUser().getId())
                .containsExactlyInAnyOrder(host.getId(), member.getId());
    }

    @Test
    @DisplayName("멤버십 잠금 조회는 상태와 무관하게 그 사람의 멤버십을 돌려준다")
    void findByGroupIdAndUserIdForUpdate() {
        assertThat(groupMemberRepository.findByGroupIdAndUserIdForUpdate(group.getId(), removed.getId()))
                .hasValueSatisfying(m -> assertThat(m.isRemoved()).isTrue());
        assertThat(groupMemberRepository.findByGroupIdAndUserIdForUpdate(group.getId(), 999L)).isEmpty();
    }

    @Test
    @DisplayName("가입 전 초대 코드로는 모임 id만 찾고, 잠근 뒤에는 초대를 잠금 조회한다")
    void invitationQueries() {
        GroupInvitation invitation = entityManager.persistAndFlush(GroupInvitation.issue(
                entityManager.find(Group.class, group.getId()), host.getId()));
        entityManager.clear();

        assertThat(groupInvitationRepository.findGroupIdByCode(invitation.getCode())).contains(group.getId());
        assertThat(groupInvitationRepository.findGroupIdByCode("missing")).isEmpty();
        assertThat(groupInvitationRepository.findByCodeForUpdate(invitation.getCode()))
                .hasValueSatisfying(found -> assertThat(found.isActive()).isTrue());
    }

    @Test
    @DisplayName("책 추가 중복 확인용 잠금 조회는 이 모임의 책만 돌려주고, 판본 비교에 필요한 책 정보를 읽을 수 있다")
    void findAllByGroupIdForUpdate() {
        Group managed = entityManager.find(Group.class, group.getId());
        Book book = book("채식주의자");
        entityManager.persistAndFlush(GroupBookEntry.create(managed, book, ReadingPeriod.NONE, LocalDate.MIN));
        Group other = entityManager.persistAndFlush(Group.create("다른 모임", entityManager.find(User.class, host.getId()), 4));
        entityManager.persistAndFlush(GroupBookEntry.create(other, book("소년이 온다"), ReadingPeriod.NONE, LocalDate.MIN));
        entityManager.clear();

        assertThat(groupBookEntryRepository.findAllByGroupIdForUpdate(group.getId()))
                .singleElement()
                .satisfies(entry -> assertThat(entry.isSameEditionAs(book)).isTrue());
    }
}
