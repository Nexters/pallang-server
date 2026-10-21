package com.nexters.palang.domain.report.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.nexters.palang.domain.report.domain.Report;
import com.nexters.palang.domain.report.domain.ReportReason;
import com.nexters.palang.domain.report.domain.ReportTargetType;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class ReportRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReportRepository reportRepository;

    private User user(String snsId) {
        return entityManager.persistAndFlush(User.builder()
                .nickname("닉네임" + snsId)
                .snsProvider(SnsProvider.KAKAO)
                .snsId(snsId)
                .termsAgreedAt(LocalDateTime.now())
                .build());
    }

    private void report(User reporter, ReportTargetType type, Long targetId, ReportReason reason) {
        entityManager.persistAndFlush(Report.of(reporter, type, targetId, reason, null));
    }

    @Test
    @DisplayName("관리자 신고 목록: 같은 대상의 신고는 하나로 묶어 신고 수와 함께 보여준다")
    void findTargetSummaries() {
        User a = user("a");
        User b = user("b");
        report(a, ReportTargetType.OPINION, 10L, ReportReason.SPAM);
        report(b, ReportTargetType.OPINION, 10L, ReportReason.ABUSE);
        report(a, ReportTargetType.COMMENT, 20L, ReportReason.HATE);

        Page<ReportTargetSummaryProjection> page = reportRepository.findTargetSummaries(PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(ReportTargetSummaryProjection::getTargetType, ReportTargetSummaryProjection::getTargetId,
                        ReportTargetSummaryProjection::getReportCount)
                .containsExactlyInAnyOrder(
                        tuple(ReportTargetType.OPINION, 10L, 2L),
                        tuple(ReportTargetType.COMMENT, 20L, 1L));
    }

    @Test
    @DisplayName("대상 id 목록으로 대상별 신고 수를 집계한다")
    void countByTargets() {
        User a = user("a");
        User b = user("b");
        report(a, ReportTargetType.OPINION, 10L, ReportReason.SPAM);
        report(b, ReportTargetType.OPINION, 10L, ReportReason.SPAM);
        report(a, ReportTargetType.OPINION, 11L, ReportReason.HATE);
        report(a, ReportTargetType.COMMENT, 10L, ReportReason.SPAM);

        List<ReportCountProjection> counts = reportRepository.countByTargets(ReportTargetType.OPINION, List.of(10L, 11L, 12L));

        assertThat(counts)
                .extracting(ReportCountProjection::getTargetId, ReportCountProjection::getReportCount)
                .containsExactlyInAnyOrder(tuple(10L, 2L), tuple(11L, 1L));
        assertThat(reportRepository.countByTargetTypeAndTargetId(ReportTargetType.OPINION, 10L)).isEqualTo(2L);
    }
}
