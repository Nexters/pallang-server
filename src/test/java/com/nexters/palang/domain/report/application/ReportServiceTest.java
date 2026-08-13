package com.nexters.palang.domain.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.comment.infrastructure.CommentRepository;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.report.common.ReportException;
import com.nexters.palang.domain.report.domain.Report;
import com.nexters.palang.domain.report.domain.ReportReason;
import com.nexters.palang.domain.report.domain.ReportTargetType;
import com.nexters.palang.domain.report.infrastructure.ReportRepository;
import com.nexters.palang.domain.report.presentation.dto.CreateReportRequest;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private OpinionRepository opinionRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    private ReportService reportService;

    private User writer;
    private User reporter;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportRepository, opinionRepository, commentRepository, userRepository);
        writer = user(10L);
        reporter = user(20L);
    }

    private User user(Long id) {
        User built = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private Opinion opinion(Long id, User author) {
        Opinion built = Opinion.builder().user(author).content("흔적 내용").build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private Comment comment(Long id, User author) {
        Comment built = Comment.root(opinion(100L, author), author, "댓글 내용");
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    @Test
    @DisplayName("존재하지 않는 흔적을 신고하면 예외가 발생한다")
    void reportOpinionFailsWhenOpinionNotFound() {
        given(opinionRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.reportOpinion(999L, reporter.getId(),
                new CreateReportRequest(ReportReason.SPAM, null)))
                .isInstanceOf(OpinionException.class);
    }

    @Test
    @DisplayName("삭제된 흔적을 신고하면 예외가 발생한다")
    void reportOpinionFailsWhenOpinionDeleted() {
        Opinion opinion = opinion(1L, writer);
        opinion.delete();
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));

        assertThatThrownBy(() -> reportService.reportOpinion(1L, reporter.getId(),
                new CreateReportRequest(ReportReason.SPAM, null)))
                .isInstanceOf(OpinionException.class);
    }

    @Test
    @DisplayName("본인이 작성한 흔적을 신고하면 예외가 발생한다")
    void reportOpinionFailsWhenSelfReport() {
        Opinion opinion = opinion(1L, writer);
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));

        assertThatThrownBy(() -> reportService.reportOpinion(1L, writer.getId(),
                new CreateReportRequest(ReportReason.SPAM, null)))
                .isInstanceOf(ReportException.class);
    }

    @Test
    @DisplayName("이미 신고한 흔적을 다시 신고하면 예외가 발생한다")
    void reportOpinionFailsWhenDuplicate() {
        Opinion opinion = opinion(1L, writer);
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporter.getId(), ReportTargetType.OPINION, 1L)).willReturn(true);

        assertThatThrownBy(() -> reportService.reportOpinion(1L, reporter.getId(),
                new CreateReportRequest(ReportReason.SPAM, null)))
                .isInstanceOf(ReportException.class);
    }

    @Test
    @DisplayName("흔적을 신고하면 신고 레코드가 저장된다")
    void reportOpinionSucceeds() {
        Opinion opinion = opinion(1L, writer);
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporter.getId(), ReportTargetType.OPINION, 1L)).willReturn(false);
        given(userRepository.getReferenceById(reporter.getId())).willReturn(reporter);
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> invocation.getArgument(0));

        Report report = reportService.reportOpinion(1L, reporter.getId(), new CreateReportRequest(ReportReason.HATE, null));

        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.OPINION);
        assertThat(report.getTargetId()).isEqualTo(1L);
        assertThat(report.getReason()).isEqualTo(ReportReason.HATE);
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 신고하면 예외가 발생한다")
    void reportCommentFailsWhenCommentNotFound() {
        given(commentRepository.findByIdWithUser(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.reportComment(999L, reporter.getId(),
                new CreateReportRequest(ReportReason.ABUSE, null)))
                .isInstanceOf(com.nexters.palang.domain.comment.common.CommentException.class);
    }

    @Test
    @DisplayName("본인이 작성한 댓글을 신고하면 예외가 발생한다")
    void reportCommentFailsWhenSelfReport() {
        Comment comment = comment(1L, writer);
        given(commentRepository.findByIdWithUser(1L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> reportService.reportComment(1L, writer.getId(),
                new CreateReportRequest(ReportReason.ABUSE, null)))
                .isInstanceOf(ReportException.class);
    }

    @Test
    @DisplayName("댓글을 신고하면 신고 레코드가 저장된다")
    void reportCommentSucceeds() {
        Comment comment = comment(1L, writer);
        given(commentRepository.findByIdWithUser(1L)).willReturn(Optional.of(comment));
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporter.getId(), ReportTargetType.COMMENT, 1L)).willReturn(false);
        given(userRepository.getReferenceById(reporter.getId())).willReturn(reporter);
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> invocation.getArgument(0));

        Report report = reportService.reportComment(1L, reporter.getId(),
                new CreateReportRequest(ReportReason.COPYRIGHT, null));

        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.COMMENT);
        assertThat(report.getTargetId()).isEqualTo(1L);
        assertThat(report.getReason()).isEqualTo(ReportReason.COPYRIGHT);
    }

    @Test
    @DisplayName("기타 사유로 신고하면서 상세 내용을 빠뜨리면 예외가 발생한다")
    void reportFailsWhenEtcReasonWithoutDetail() {
        Opinion opinion = opinion(1L, writer);
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporter.getId(), ReportTargetType.OPINION, 1L)).willReturn(false);
        given(userRepository.getReferenceById(reporter.getId())).willReturn(reporter);

        assertThatThrownBy(() -> reportService.reportOpinion(1L, reporter.getId(),
                new CreateReportRequest(ReportReason.ETC, null)))
                .isInstanceOf(ReportException.class);
    }
}
