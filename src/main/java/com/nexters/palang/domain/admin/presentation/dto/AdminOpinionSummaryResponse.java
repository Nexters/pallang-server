package com.nexters.palang.domain.admin.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminOpinionSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long opinionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long passageId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String passageQuotedText,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String userNickname,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int likeCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean deleted,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
}
