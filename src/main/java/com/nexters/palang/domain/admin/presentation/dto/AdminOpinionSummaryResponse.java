package com.nexters.palang.domain.admin.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminOpinionSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long opinionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long passageId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String passageQuotedText,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bookTitle,
        @Schema(nullable = true, description = "모임 전용 대목에 달린 의견이면 그 모임 id, 전역 공개면 null") Long groupId,
        @Schema(nullable = true) String groupName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String userNickname,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int likeCount,
        @Schema(description = "이 의견에 접수된 신고 수", requiredMode = Schema.RequiredMode.REQUIRED) long reportCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean deleted,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
}
