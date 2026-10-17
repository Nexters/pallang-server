package com.nexters.palang.domain.group.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberRole;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class GroupMemberRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    private User user(String snsId) {
        return entityManager.persistAndFlush(User.builder()
                .nickname("닉네임" + snsId)
                .snsProvider(SnsProvider.KAKAO)
                .snsId(snsId)
                .build());
    }

    private Book book() {
        return entityManager.persistAndFlush(Book.builder()
                .title("채식주의자").author("한강").publisher("창비").pageCount(268).source(BookSource.API).build());
    }

    private Group group(Book book, User host) {
        Group built = Group.create("모임", book, host, 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));
        return entityManager.persistAndFlush(built);
    }

    @Test
    @DisplayName("모임 알림 팬아웃을 위해 멤버 전체의 user id를 조회한다")
    void findUserIdsByGroupId() {
        Book book = book();
        User host = user("host");
        User member = user("member");
        Group group = group(book, host);
        entityManager.persistAndFlush(GroupMember.of(group, host, GroupMemberRole.HOST));
        entityManager.persistAndFlush(GroupMember.of(group, member, GroupMemberRole.MEMBER));

        var userIds = groupMemberRepository.findUserIdsByGroupId(group.getId());

        assertThat(userIds).containsExactlyInAnyOrder(host.getId(), member.getId());
    }

    @Test
    @DisplayName("다른 모임의 멤버는 포함하지 않는다")
    void findUserIdsByGroupIdExcludesOtherGroups() {
        Book book = book();
        User host = user("host");
        User otherHost = user("otherHost");
        Group group = group(book, host);
        Group otherGroup = group(book, otherHost);
        entityManager.persistAndFlush(GroupMember.of(group, host, GroupMemberRole.HOST));
        entityManager.persistAndFlush(GroupMember.of(otherGroup, otherHost, GroupMemberRole.HOST));

        var userIds = groupMemberRepository.findUserIdsByGroupId(group.getId());

        assertThat(userIds).containsExactly(host.getId());
    }
}
