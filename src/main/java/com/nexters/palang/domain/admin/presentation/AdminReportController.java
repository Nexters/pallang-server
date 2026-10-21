package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.application.AdminAccessGuard;
import com.nexters.palang.domain.admin.application.AdminReportService;
import com.nexters.palang.domain.admin.presentation.dto.AdminReportListResponse;
import com.nexters.palang.global.common.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminReportController implements AdminReportApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AdminAccessGuard adminAccessGuard;
    private final AdminReportService adminReportService;

    @Override
    @GetMapping("/api/admin/reports")
    public ResponseEntity<DataResponse<AdminReportListResponse>> getReportedTargets(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        adminAccessGuard.requireAdmin();
        return ResponseEntity.ok(DataResponse.from(adminReportService.getReportedTargets(
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE)))));
    }
}
