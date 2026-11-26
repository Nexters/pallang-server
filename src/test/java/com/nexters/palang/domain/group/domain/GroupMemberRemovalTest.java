package com.nexters.palang.domain.group.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.user.domain.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

class GroupMemberRemovalTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 12, 0);

    private User user(Long id) {
        User built = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private GroupInvitation invitationIssuedAt(Group group, LocalDateTime issuedAt) {
        GroupInvitation invitation = GroupInvitation.issue(group, 1L);
        ReflectionTestUtils.setField(invitation, "createdAt", issuedAt);
        return invitation;
    }

    // ===== 사유 =====

    @ParameterizedTest
    @EnumSource(value = GroupMemberRemovalReason.class, names = "OTHER", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("기타가 아닌 사유는 직접 입력 없이 기록된다")
    void recordsReasonWithoutOtherText(GroupMemberRemovalReason reason) {
        GroupMemberRemoval removal = GroupMemberRemoval.record(9L, 1L, 2L, reason, null, "key");

        assertThat(removal.getReason()).isEqualTo(reason);
        assertThat(removal.getReasonCode()).isEqualTo(reason.name());
        assertThat(removal.getOtherText()).isNull();
    }

    @Test
    @DisplayName("기타가 아닌 사유에 직접 입력이 오면 예외가 발생한다")
    void rejectsOtherTextForNonOtherReason() {
        assertThatThrownBy(() -> GroupMemberRemoval.record(
                9L, 1L, 2L, GroupMemberRemovalReason.ABUSIVE_BEHAVIOR, "추가 설명", null))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.INVALID_REMOVAL_REASON);
    }

    @Test
    @DisplayName("기타 사유는 앞뒤 공백을 뺀 직접 입력과 함께 기록된다")
    void recordsOtherWithStrippedText() {
        GroupMemberRemoval removal = GroupMemberRemoval.record(
                9L, 1L, 2L, GroupMemberRemovalReason.OTHER, "  모임 규칙을 반복해서 어김  ", null);

        assertThat(removal.getOtherText()).isEqualTo("모임 규칙을 반복해서 어김");
    }

    @Test
    @DisplayName("기타 사유인데 내용이 비었거나 공백뿐이면 예외가 발생한다")
    void rejectsBlankOtherText() {
        assertThatThrownBy(() -> GroupMemberRemoval.record(9L, 1L, 2L, GroupMemberRemovalReason.OTHER, "   ", null))
                .isInstanceOf(GroupException.class);
        assertThatThrownBy(() -> GroupMemberRemoval.record(9L, 1L, 2L, GroupMemberRemovalReason.OTHER, null, null))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("기타 사유 내용은 보이는 글자 기준 200자까지 허용하고 201자는 거절한다")
    void otherTextLimitIs200Graphemes() {
        String emoji = "👍🏽"; // 피부색 이모지: UTF-16 4자지만 1자로 센다
        String twoHundred = emoji.repeat(200);

        assertThat(GroupMemberRemoval.record(9L, 1L, 2L, GroupMemberRemovalReason.OTHER, twoHundred, null).getOtherText())
                .isEqualTo(twoHundred);
        assertThatThrownBy(() -> GroupMemberRemoval.record(
                9L, 1L, 2L, GroupMemberRemovalReason.OTHER, twoHundred + "가", null))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("기타 사유 길이는 내부 공백을 빼고 세고, 저장은 앞뒤 공백만 뺀 원문 그대로 한다")
    void otherTextLengthExcludesInnerWhitespace() {
        String text = "가" + " ".repeat(300) + "나"; // 공백 제외 2자
        String fullWidthSpaced = "가　나"; // 전각 공백도 공백으로 본다

        assertThat(GroupMemberRemoval.record(9L, 1L, 2L, GroupMemberRemovalReason.OTHER, text, null).getOtherText())
                .isEqualTo(text);
        assertThat(GroupMemberRemoval.record(9L, 1L, 2L, GroupMemberRemovalReason.OTHER, fullWidthSpaced, null)
                .getOtherText()).isEqualTo(fullWidthSpaced);
        assertThatThrownBy(() -> GroupMemberRemoval.record(
                9L, 1L, 2L, GroupMemberRemovalReason.OTHER, "　 \t　", null))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("코드 포인트가 많은 이모지 200자도 저장 길이 제한에 걸리지 않는다")
    void allowsTwoHundredZwjEmojis() {
        // 가족 이모지: 보이는 글자 1개, 코드 포인트 7개, UTF-16 11자
        String family = "👨‍👩‍👧‍👦";
        String twoHundred = family.repeat(200);

        assertThat(GroupMemberRemoval.record(9L, 1L, 2L, GroupMemberRemovalReason.OTHER, twoHundred, null).getOtherText())
                .isEqualTo(twoHundred);
    }

    @Test
    @DisplayName("사유가 없으면 예외가 발생한다")
    void rejectsMissingReason() {
        assertThatThrownBy(() -> GroupMemberRemoval.record(9L, 1L, 2L, null, null, null))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.INVALID_REMOVAL_REASON);
    }

    // ===== 멤버십 =====

    @Test
    @DisplayName("내보내면 멤버십이 REMOVED가 되고 내보낸 시각이 남는다")
    void removeMarksMembershipRemoved() {
        GroupMember membership = GroupMember.join(
                Group.create("모임", user(1L), 4), user(2L), GroupMemberRole.MEMBER, NOW.minusDays(10));

        membership.remove(NOW);

        assertThat(membership.isActive()).isFalse();
        assertThat(membership.isRemoved()).isTrue();
        assertThat(membership.getRemovedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("이미 나간 멤버십은 내보내도 상태와 시각이 바뀌지 않는다")
    void removeDoesNothingForInactiveMembership() {
        GroupMember membership = GroupMember.join(
                Group.create("모임", user(1L), 4), user(2L), GroupMemberRole.MEMBER, NOW.minusDays(10));
        ReflectionTestUtils.setField(membership, "status", GroupMemberStatus.LEFT);

        membership.remove(NOW);

        assertThat(membership.getStatus()).isEqualTo(GroupMemberStatus.LEFT);
        assertThat(membership.getRemovedAt()).isNull();
    }

    @Test
    @DisplayName("내보내진 계정은 내보낸 시각 이전(같은 시각 포함)에 발급된 초대로는 다시 들어올 수 없다")
    void removedMemberCannotRejoinWithOldInvitation() {
        Group group = Group.create("모임", user(1L), 4);
        GroupMember membership = GroupMember.join(group, user(2L), GroupMemberRole.MEMBER, NOW.minusDays(10));
        membership.remove(NOW);

        assertThat(membership.canRejoinWith(invitationIssuedAt(group, NOW.minusDays(1)))).isFalse();
        assertThat(membership.canRejoinWith(invitationIssuedAt(group, NOW))).isFalse();
        assertThat(membership.canRejoinWith(invitationIssuedAt(group, NOW.plusSeconds(1)))).isTrue();
    }

    @Test
    @DisplayName("스스로 나간(LEFT) 계정은 예전 초대로도 다시 들어올 수 있다")
    void leftMemberCanRejoinWithAnyInvitation() {
        Group group = Group.create("모임", user(1L), 4);
        GroupMember membership = GroupMember.join(group, user(2L), GroupMemberRole.MEMBER, NOW.minusDays(10));
        ReflectionTestUtils.setField(membership, "status", GroupMemberStatus.LEFT);

        assertThat(membership.canRejoinWith(invitationIssuedAt(group, NOW.minusDays(30)))).isTrue();
    }
}
