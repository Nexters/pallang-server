package com.nexters.palang.domain.report.presentation.dto;

import com.nexters.palang.domain.report.domain.Report;
import com.nexters.palang.domain.report.domain.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotNull(message = "신고 사유는 필수입니다.")
        @Schema(example = "SPAM", requiredMode = Schema.RequiredMode.REQUIRED)
        ReportReason reason,
        @Size(max = Report.DETAIL_MAX_LENGTH, message = "상세 내용은 500자를 초과할 수 없습니다.")
        @Schema(description = "기타 사유 선택 시 필수", example = "같은 광고 링크를 반복해서 남겼어요.", nullable = true)
        String detail
) {
}
