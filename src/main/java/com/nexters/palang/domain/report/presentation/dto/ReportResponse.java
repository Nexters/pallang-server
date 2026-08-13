package com.nexters.palang.domain.report.presentation.dto;

import com.nexters.palang.domain.report.domain.Report;
import com.nexters.palang.domain.report.domain.ReportReason;
import com.nexters.palang.domain.report.domain.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ReportResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long reportId,
        @Schema(example = "OPINION", requiredMode = Schema.RequiredMode.REQUIRED) ReportTargetType targetType,
        @Schema(example = "10", requiredMode = Schema.RequiredMode.REQUIRED) Long targetId,
        @Schema(example = "SPAM", requiredMode = Schema.RequiredMode.REQUIRED) ReportReason reason,
        @Schema(example = "같은 광고 링크를 반복해서 남겼어요.", nullable = true) String detail,
        @Schema(example = "2026-08-05T14:32:00", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getDetail(),
                report.getCreatedAt());
    }
}
