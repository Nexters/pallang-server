package com.nexters.palang.domain.report.infrastructure;

import com.nexters.palang.domain.report.domain.Report;
import com.nexters.palang.domain.report.domain.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    // 관리자 유저 삭제(AdminUserService): 이 유저가 신고자인 신고 내역 전부.
    void deleteAllByReporterId(Long reporterId);
}
