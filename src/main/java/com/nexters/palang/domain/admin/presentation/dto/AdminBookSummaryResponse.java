package com.nexters.palang.domain.admin.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminBookSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String author,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String publisher,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int pageCount,
        @Schema(nullable = true) String isbn,
        @Schema(nullable = true) String coverImageUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String source,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
}
