package com.nexters.palang.domain.admin.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminPassageSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long passageId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bookTitle,
        @Schema(nullable = true, description = "모임 전용 대목이면 모임 id, 전역 공개 대목이면 null") Long groupId,
        @Schema(nullable = true) String groupName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long creatorId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String creatorNickname,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int pageNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String quotedText,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean isSpoiler,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean deleted,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
}
