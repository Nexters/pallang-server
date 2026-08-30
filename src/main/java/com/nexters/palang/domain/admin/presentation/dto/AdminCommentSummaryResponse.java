package com.nexters.palang.domain.admin.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminCommentSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long commentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long opinionId,
        @Schema(nullable = true, description = "답글이면 원댓글 id, 원댓글이면 null") Long parentCommentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String userNickname,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean deleted,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
}
