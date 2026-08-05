package com.nexters.palang.domain.report.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.domain.report.common.ReportException;
import com.nexters.palang.domain.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportTest {

    @Test
    @DisplayName("기타 사유가 아니면 상세 내용 없이도 신고를 생성할 수 있다")
    void createsReportWithoutDetailWhenReasonIsNotEtc() {
        User reporter = User.builder().build();

        Report report = Report.of(reporter, ReportTargetType.OPINION, 1L, ReportReason.SPAM, null);

        assertThat(report.getReason()).isEqualTo(ReportReason.SPAM);
        assertThat(report.getDetail()).isNull();
    }

    @Test
    @DisplayName("기타 사유인데 상세 내용이 없으면 예외가 발생한다")
    void throwsWhenReasonIsEtcAndDetailIsMissing() {
        User reporter = User.builder().build();

        assertThatThrownBy(() -> Report.of(reporter, ReportTargetType.OPINION, 1L, ReportReason.ETC, null))
                .isInstanceOf(ReportException.class);
        assertThatThrownBy(() -> Report.of(reporter, ReportTargetType.OPINION, 1L, ReportReason.ETC, "   "))
                .isInstanceOf(ReportException.class);
    }

    @Test
    @DisplayName("기타 사유이고 상세 내용이 있으면 신고를 생성할 수 있다")
    void createsReportWhenReasonIsEtcAndDetailIsProvided() {
        User reporter = User.builder().build();

        Report report = Report.of(reporter, ReportTargetType.COMMENT, 2L, ReportReason.ETC, "이유 설명");

        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.COMMENT);
        assertThat(report.getTargetId()).isEqualTo(2L);
        assertThat(report.getDetail()).isEqualTo("이유 설명");
    }
}
