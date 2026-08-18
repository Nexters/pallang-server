package com.nexters.palang.domain.group.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record GroupSummaryResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long groupId,
        @Schema(example = "고전 뽀개기", requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long bookId,
        @Schema(example = "채식주의자", requiredMode = Schema.RequiredMode.REQUIRED) String bookTitle,
        @Schema(example = "https://image.aladin.co.kr/product/123/45/cover/8936434120_1.jpg", nullable = true) String bookCoverImageUrl,
        @Schema(example = "2", requiredMode = Schema.RequiredMode.REQUIRED) long memberCount,
        @Schema(example = "4", requiredMode = Schema.RequiredMode.REQUIRED) int capacity,
        @Schema(example = "2026-08-20", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate startDate,
        @Schema(example = "2026-09-20", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate endDate,
        @Schema(example = "false", requiredMode = Schema.RequiredMode.REQUIRED) boolean ended
) {
}
