package com.nexters.palang.domain.report.application;

import com.nexters.palang.domain.comment.common.CommentErrorCode;
import com.nexters.palang.domain.comment.common.CommentException;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.comment.infrastructure.CommentRepository;
import com.nexters.palang.domain.opinion.common.error.OpinionErrorCode;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.report.common.ReportErrorCode;
import com.nexters.palang.domain.report.common.ReportException;
import com.nexters.palang.domain.report.domain.Report;
import com.nexters.palang.domain.report.domain.ReportTargetType;
import com.nexters.palang.domain.report.infrastructure.ReportRepository;
import com.nexters.palang.domain.report.presentation.dto.CreateReportRequest;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final OpinionRepository opinionRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Transactional
    public Report reportOpinion(Long opinionId, Long reporterId, CreateReportRequest request) {
        Opinion opinion = getExistingOpinion(opinionId);
        validateNotSelf(opinion.getUser().getId(), reporterId);
        return saveReport(reporterId, ReportTargetType.OPINION, opinionId, request);
    }

    @Transactional
    public Report reportComment(Long commentId, Long reporterId, CreateReportRequest request) {
        Comment comment = getExistingComment(commentId);
        validateNotSelf(comment.getUser().getId(), reporterId);
        return saveReport(reporterId, ReportTargetType.COMMENT, commentId, request);
    }

    private Report saveReport(Long reporterId, ReportTargetType targetType, Long targetId, CreateReportRequest request) {
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, targetType, targetId)) {
            throw new ReportException(ReportErrorCode.DUPLICATE_REPORT);
        }
        User reporter = userRepository.getReferenceById(reporterId);
        Report report = Report.of(reporter, targetType, targetId, request.reason(), request.detail());
        return reportRepository.save(report);
    }

    private void validateNotSelf(Long targetOwnerId, Long reporterId) {
        if (targetOwnerId.equals(reporterId)) {
            throw new ReportException(ReportErrorCode.SELF_REPORT_NOT_ALLOWED);
        }
    }

    private Opinion getExistingOpinion(Long opinionId) {
        Opinion opinion = opinionRepository.findById(opinionId)
                .orElseThrow(() -> new OpinionException(OpinionErrorCode.OPINION_NOT_FOUND));
        if (opinion.isDeleted()) {
            throw new OpinionException(OpinionErrorCode.OPINION_NOT_FOUND);
        }
        return opinion;
    }

    private Comment getExistingComment(Long commentId) {
        Comment comment = commentRepository.findByIdWithUser(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));
        if (comment.isDeleted()) {
            throw new CommentException(CommentErrorCode.COMMENT_NOT_FOUND);
        }
        return comment;
    }
}
