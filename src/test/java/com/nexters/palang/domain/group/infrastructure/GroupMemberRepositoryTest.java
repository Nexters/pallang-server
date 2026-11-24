package com.nexters.palang.domain.group.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberRole;
import com.nexters.palang.domain.group.domain.GroupMemberStatus;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class GroupMemberRepositoryTest {

    private static final LocalDateTime JOINED_AT = LocalDateTime.of(2026, 8, 1, 12, 0);

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

    private Group group(User host) {
        return entityManager.persistAndFlush(Group.create("모임", host, 4));
    }

    private GroupMember join(Group group, User user, GroupMemberRole role) {
        return entityManager.persistAndFlush(GroupMember.join(group, user, role, JOINED_AT));
    }

    // 내보내기 기능(#226) 전이라 상태를 바꾸는 도메인 메서드가 없어, 이력이 남은 멤버십을 직접 만든다.
    private void markRemoved(GroupMember membership) {
        ReflectionTestUtils.setField(membership, "status", GroupMemberStatus.REMOVED);
        entityManager.persistAndFlush(membership);
    }

    @Test
    @DisplayName("모임 알림 팬아웃을 위해 현재 멤버 전체의 user id를 조회한다")
    void findActiveUserIdsByGroupId() {
        User host = user("host");
        User member = user("member");
        Group group = group(host);
        join(group, host, GroupMemberRole.HOST);
        join(group, member, GroupMemberRole.MEMBER);

        var userIds = groupMemberRepository.findActiveUserIdsByGroupId(group.getId());

        assertThat(userIds).containsExactlyInAnyOrder(host.getId(), member.getId());
    }

    @Test
    @DisplayName("다른 모임의 멤버는 포함하지 않는다")
    void findActiveUserIdsByGroupIdExcludesOtherGroups() {
        User host = user("host");
        User otherHost = user("otherHost");
        Group group = group(host);
        Group otherGroup = group(otherHost);
        join(group, host, GroupMemberRole.HOST);
        join(otherGroup, otherHost, GroupMemberRole.HOST);

        var userIds = groupMemberRepository.findActiveUserIdsByGroupId(group.getId());

        assertThat(userIds).containsExactly(host.getId());
    }

    @Test
    @DisplayName("내보내진 멤버는 인원 수, 멤버 여부, 알림 대상에서 모두 빠진다")
    void removedMemberIsExcludedFromActiveQueries() {
        User host = user("host");
        User removed = user("removed");
        Group group = group(host);
        join(group, host, GroupMemberRole.HOST);
        markRemoved(join(group, removed, GroupMemberRole.MEMBER));

        assertThat(groupMemberRepository.countActiveMembers(group.getId())).isEqualTo(1L);
        assertThat(groupMemberRepository.existsActiveMember(group.getId(), removed.getId())).isFalse();
        assertThat(groupMemberRepository.findActiveUserIdsByGroupId(group.getId())).containsExactly(host.getId());
        assertThat(groupMemberRepository.findByGroupIdAndUserId(group.getId(), removed.getId())).isPresent();
    }
}
