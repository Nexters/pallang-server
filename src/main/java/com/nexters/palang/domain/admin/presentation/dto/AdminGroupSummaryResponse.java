package com.nexters.palang.domain.admin.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminGroupSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long groupId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bookTitle,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long hostId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String hostNickname,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int capacity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long memberCount,
        @Schema(nullable = true, description = "대표 책(가장 먼저 등록된 책)의 시작일. 기간이 없으면 null.") LocalDate startDate,
        @Schema(nullable = true, description = "대표 책(가장 먼저 등록된 책)의 종료일. 기간이 없으면 null.") LocalDate endDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
}
