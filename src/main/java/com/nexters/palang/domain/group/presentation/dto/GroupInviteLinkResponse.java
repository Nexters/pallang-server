package com.nexters.palang.domain.group.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GroupInviteLinkResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long groupId,
        @Schema(example = "a1b2c3d4e5f64789a0b1c2d3e4f5a6b7", description = "초대 코드. 클라이언트가 이 값으로 딥링크를 구성해 카카오톡 등으로 공유한다.",
                requiredMode = Schema.RequiredMode.REQUIRED) String inviteCode,
        @Schema(example = "5", description = "현재 멤버 수", requiredMode = Schema.RequiredMode.REQUIRED) long memberCount,
        @Schema(example = "10", description = "모임 정원", requiredMode = Schema.RequiredMode.REQUIRED) int capacity,
        @Schema(example = "false", description = "정원이 가득 찼는지 여부. true면 공유 전에 정원 안내를 보여주고, 정원 늘리기는 "
                + "방 설정 변경을 쓴다(링크를 받아도 가입 시점에 다시 정원을 확인한다).",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean full
) {
}
