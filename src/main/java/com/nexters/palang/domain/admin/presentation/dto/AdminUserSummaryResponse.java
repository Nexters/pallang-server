package com.nexters.palang.domain.admin.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminUserSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
        @Schema(nullable = true) String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String snsProvider,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean withdrawn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                description = "이 유저가 호스트인 모임 수. 1개 이상이면 그 모임에 다른 멤버가 있는지에 따라 "
                        + "삭제 요청이 차단(409)될 수 있다.")
        long hostedGroupCount
) {
}
