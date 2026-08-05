package com.nexters.palang.domain.report.presentation;

import com.nexters.palang.domain.report.application.ReportService;
import com.nexters.palang.domain.report.domain.Report;
import com.nexters.palang.domain.report.presentation.dto.CreateReportRequest;
import com.nexters.palang.domain.report.presentation.dto.ReportResponse;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportController implements ReportApi {

    private final ReportService reportService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @PostMapping("/api/opinions/{opinionId}/reports")
    public ResponseEntity<DataResponse<ReportResponse>> reportOpinion(
            @PathVariable Long opinionId,
            @RequestBody @Valid CreateReportRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Report report = reportService.reportOpinion(opinionId, currentUserId, request);
        return ResponseEntity.ok(DataResponse.from(ReportResponse.from(report)));
    }

    @Override
    @PostMapping("/api/comments/{commentId}/reports")
    public ResponseEntity<DataResponse<ReportResponse>> reportComment(
            @PathVariable Long commentId,
            @RequestBody @Valid CreateReportRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Report report = reportService.reportComment(commentId, currentUserId, request);
        return ResponseEntity.ok(DataResponse.from(ReportResponse.from(report)));
    }
}
