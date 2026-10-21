package com.nexters.palang.domain.report.infrastructure;

import com.nexters.palang.domain.report.domain.ReportTargetType;
import java.time.LocalDateTime;

public interface ReportTargetSummaryProjection {

    ReportTargetType getTargetType();

    Long getTargetId();

    Long getReportCount();

    LocalDateTime getLastReportedAt();
}
