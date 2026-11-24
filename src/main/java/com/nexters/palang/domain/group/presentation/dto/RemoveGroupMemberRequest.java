package com.nexters.palang.domain.group.presentation.dto;

import com.nexters.palang.domain.group.domain.GroupMemberRemovalReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// 멤버 내보내기. 사유는 1개 필수. OTHER(기타)면 otherText에 공백 제외 1~200자를 넣고, 그 외 사유면 otherText를
// 보내지 않는다.
public record RemoveGroupMemberRequest(
        @NotNull(message = "내보내기 사유를 선택해 주세요.")
        @Schema(example = "SPAM_OR_OFF_TOPIC", description = "ABUSIVE_BEHAVIOR: 욕설·비방 등 다른 멤버에게 불편을 주는 행동 / "
                + "SPAM_OR_OFF_TOPIC: 광고·홍보 또는 모임과 무관한 내용 반복 게시 / "
                + "MEMBER_REQUEST_OR_MISTAKE: 잘못 참여했거나 본인이 나가기를 요청함 / OTHER: 기타(직접입력)")
        GroupMemberRemovalReason reasonCode,

        @Schema(example = "모임 규칙을 반복해서 지키지 않음", nullable = true,
                description = "reasonCode가 OTHER일 때만, 공백 제외 1~200자(이모지 등은 보이는 글자 1개를 1자로 셈)")
        String otherText
) {
}
