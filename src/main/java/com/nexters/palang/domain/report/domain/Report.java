package com.nexters.palang.domain.report.domain;

import com.nexters.palang.domain.report.common.ReportErrorCode;
import com.nexters.palang.domain.report.common.ReportException;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_reports_reporter_target",
                columnNames = {"reporter_id", "target_type", "target_id"}),
        indexes = {
                @Index(name = "idx_reports_target", columnList = "target_type, target_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

    public static final int DETAIL_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private ReportReason reason;

    @Column(name = "detail", length = DETAIL_MAX_LENGTH)
    private String detail;

    private Report(User reporter, ReportTargetType targetType, Long targetId, ReportReason reason, String detail) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.detail = detail;
    }

    // 기타 사유는 상세 내용 없이는 신고 검토에 쓸모가 없으므로 생성 시점에 막는다.
    public static Report of(User reporter, ReportTargetType targetType, Long targetId, ReportReason reason, String detail) {
        if (reason == ReportReason.ETC && (detail == null || detail.isBlank())) {
            throw new ReportException(ReportErrorCode.DETAIL_REQUIRED_FOR_ETC);
        }
        return new Report(reporter, targetType, targetId, reason, detail);
    }
}
