package com.nexters.palang.domain.group.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GroupInviteLinkResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long groupId,
        @Schema(example = "a1b2c3d4e5f64789a0b1c2d3e4f5a6b7", description = "초대 코드. 클라이언트가 이 값으로 딥링크를 구성해 카카오톡 등으로 공유한다.",
                requiredMode = Schema.RequiredMode.REQUIRED) String inviteCode
) {
}
