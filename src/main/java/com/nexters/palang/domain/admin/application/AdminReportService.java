package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.admin.presentation.dto.AdminReportListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminReportedTargetResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminReportedTargetResponse.ReasonCount;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.comment.infrastructure.CommentRepository;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.report.domain.Report;
import com.nexters.palang.domain.report.domain.ReportReason;
import com.nexters.palang.domain.report.domain.ReportTargetType;
import com.nexters.palang.domain.report.infrastructure.ReportRepository;
import com.nexters.palang.domain.report.infrastructure.ReportTargetSummaryProjection;
import com.nexters.palang.global.common.response.PageInfo;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 "신고" 메뉴(이슈 #208): 신고를 대상(의견/댓글) 단위로 묶어 최근 신고 순으로 보여준다. 대상의 내용과
// 맥락(대목·책·모임)을 함께 보여줘야 사유 선택 삭제 여부를 판단할 수 있다. 연관을 지연 로딩으로 읽으므로
// 응답 조립까지 이 트랜잭션 안에서 끝낸다(open-in-view: false).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    private static final int MAX_DETAILS = 5;

    private final ReportRepository reportRepository;
    private final OpinionRepository opinionRepository;
    private final CommentRepository commentRepository;

    public AdminReportListResponse getReportedTargets(AdminSortOrder sort, Pageable pageable) {
        Page<ReportTargetSummaryProjection> summaries = sort == AdminSortOrder.OLDEST
                ? reportRepository.findTargetSummariesOldestFirst(pageable)
                : reportRepository.findTargetSummaries(pageable);

        List<Long> opinionIds = targetIds(summaries, ReportTargetType.OPINION);
        List<Long> commentIds = targetIds(summaries, ReportTargetType.COMMENT);
        Map<Long, Opinion> opinions = opinionRepository.findAllById(opinionIds).stream()
                .collect(Collectors.toMap(Opinion::getId, Function.identity()));
        Map<Long, Comment> comments = commentRepository.findAllById(commentIds).stream()
                .collect(Collectors.toMap(Comment::getId, Function.identity()));
        Map<ReportTargetType, Map<Long, List<Report>>> reports = new EnumMap<>(ReportTargetType.class);
        reports.put(ReportTargetType.OPINION, reportsByTarget(ReportTargetType.OPINION, opinionIds));
        reports.put(ReportTargetType.COMMENT, reportsByTarget(ReportTargetType.COMMENT, commentIds));

        List<AdminReportedTargetResponse> content = summaries.getContent().stream()
                .map(summary -> toResponse(summary,
                        reports.get(summary.getTargetType()).getOrDefault(summary.getTargetId(), List.of()),
                        opinions, comments))
                .toList();
        return new AdminReportListResponse(content, PageInfo.from(summaries));
    }

    private List<Long> targetIds(Page<ReportTargetSummaryProjection> summaries, ReportTargetType type) {
        return summaries.getContent().stream()
                .filter(summary -> summary.getTargetType() == type)
                .map(ReportTargetSummaryProjection::getTargetId)
                .toList();
    }

    private Map<Long, List<Report>> reportsByTarget(ReportTargetType type, List<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return Map.of();
        }
        return reportRepository.findByTargetTypeAndTargetIdIn(type, targetIds).stream()
                .collect(Collectors.groupingBy(Report::getTargetId));
    }

    private AdminReportedTargetResponse toResponse(
            ReportTargetSummaryProjection summary, List<Report> reports,
            Map<Long, Opinion> opinions, Map<Long, Comment> comments) {
        List<ReasonCount> reasons = reports.stream()
                .collect(Collectors.groupingBy(Report::getReason, () -> new EnumMap<>(ReportReason.class),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<ReportReason, Long>comparingByValue().reversed())
                .map(entry -> new ReasonCount(entry.getKey(), entry.getValue()))
                .toList();
        List<String> details = reports.stream()
                .sorted(Comparator.comparing(Report::getCreatedAt).reversed())
                .map(Report::getDetail)
                .filter(detail -> detail != null && !detail.isBlank())
                .limit(MAX_DETAILS)
                .toList();

        Opinion opinion = null;
        String authorNickname = null;
        String content = null;
        if (summary.getTargetType() == ReportTargetType.OPINION) {
            Opinion target = opinions.get(summary.getTargetId());
            if (target != null && !target.isDeleted()) {
                opinion = target;
                authorNickname = target.getUser().getNickname();
                content = target.getContent();
            }
        } else {
            Comment target = comments.get(summary.getTargetId());
            if (target != null && !target.isDeleted()) {
                opinion = target.getOpinion();
                authorNickname = target.getUser().getNickname();
                content = target.getContent();
            }
        }

        Group group = opinion != null ? opinion.getPassage().getGroup() : null;
        return new AdminReportedTargetResponse(
                summary.getTargetType(), summary.getTargetId(), summary.getReportCount(), summary.getLastReportedAt(),
                reasons, details, opinion != null, authorNickname, content,
                opinion != null ? opinion.getId() : null,
                opinion != null ? opinion.getPassage().getQuotedText() : null,
                opinion != null ? opinion.getPassage().getBook().getTitle() : null,
                group != null ? group.getName() : null);
    }
}
