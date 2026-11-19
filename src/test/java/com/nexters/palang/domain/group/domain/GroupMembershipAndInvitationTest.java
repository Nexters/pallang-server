package com.nexters.palang.domain.group.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.user.domain.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GroupMembershipAndInvitationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 19, 12, 0);

    private User user(Long id) {
        User built = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private Group group() {
        return Group.create("고전 뽀개기", user(1L), 4);
    }

    @Test
    @DisplayName("가입하면 참여일이 기록된 ACTIVE 멤버십이 된다")
    void joinCreatesActiveMembership() {
        GroupMember membership = GroupMember.join(group(), user(2L), GroupMemberRole.MEMBER, NOW);

        assertThat(membership.isActive()).isTrue();
        assertThat(membership.getJoinedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("탈퇴했던 멤버가 다시 가입하면 같은 멤버십이 ACTIVE로 돌아오고 참여일이 갱신된다")
    void rejoinReactivatesMembership() {
        GroupMember membership = GroupMember.join(group(), user(2L), GroupMemberRole.MEMBER, NOW);
        ReflectionTestUtils.setField(membership, "status", GroupMemberStatus.LEFT);

        membership.rejoin(NOW.plusDays(3));

        assertThat(membership.isActive()).isTrue();
        assertThat(membership.getJoinedAt()).isEqualTo(NOW.plusDays(3));
    }

    @Test
    @DisplayName("초대를 발급하면 32자 코드의 ACTIVE 초대가 된다")
    void issuesInvitation() {
        GroupInvitation invitation = GroupInvitation.issue(group(), 1L);

        assertThat(invitation.isActive()).isTrue();
        assertThat(invitation.getCode()).hasSize(32);
        assertThat(invitation.getIssuedBy()).isEqualTo(1L);
    }

    @Test
    @DisplayName("초대를 종료하면 더 이상 유효하지 않고 종료 시각이 남는다")
    void revokesInvitation() {
        GroupInvitation invitation = GroupInvitation.issue(group(), 1L);

        invitation.revoke(NOW);

        assertThat(invitation.isActive()).isFalse();
        assertThat(invitation.getRevokedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("이미 종료된 초대를 다시 종료해도 처음 종료 시각이 유지된다")
    void revokeIsIdempotent() {
        GroupInvitation invitation = GroupInvitation.issue(group(), 1L);
        invitation.revoke(NOW);

        invitation.revoke(NOW.plusDays(1));

        assertThat(invitation.getRevokedAt()).isEqualTo(NOW);
    }
}
