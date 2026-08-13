package com.nexters.palang.domain.report.infrastructure;

import com.nexters.palang.domain.report.domain.Report;
import com.nexters.palang.domain.report.domain.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);
}
