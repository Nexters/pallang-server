package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.application.AdminAccessGuard;
import com.nexters.palang.domain.admin.application.AdminOpinionMapper;
import com.nexters.palang.domain.admin.application.AdminOpinionService;
import com.nexters.palang.domain.admin.presentation.dto.AdminOpinionListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminOpinionSummaryResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminUpdateOpinionRequest;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.global.common.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminOpinionController implements AdminOpinionApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AdminAccessGuard adminAccessGuard;
    private final AdminOpinionService adminOpinionService;

    @Override
    @GetMapping("/api/admin/opinions")
    public ResponseEntity<DataResponse<AdminOpinionListResponse>> searchOpinions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        adminAccessGuard.requireAdmin();
        Page<Opinion> results = adminOpinionService.searchOpinions(keyword, pageable(page, size));
        return ResponseEntity.ok(DataResponse.from(AdminOpinionMapper.toListResponse(results)));
    }

    @Override
    @PatchMapping("/api/admin/opinions/{opinionId}")
    public ResponseEntity<DataResponse<AdminOpinionSummaryResponse>> updateOpinion(
            @PathVariable Long opinionId, @Valid @RequestBody AdminUpdateOpinionRequest request) {
        adminAccessGuard.requireAdmin();
        Opinion opinion = adminOpinionService.updateOpinion(opinionId, request.content());
        return ResponseEntity.ok(DataResponse.from(AdminOpinionMapper.toSummary(opinion)));
    }

    @Override
    @DeleteMapping("/api/admin/opinions/{opinionId}")
    public ResponseEntity<DataResponse<Void>> deleteOpinion(@PathVariable Long opinionId) {
        adminAccessGuard.requireAdmin();
        adminOpinionService.deleteOpinion(opinionId);
        return ResponseEntity.ok(DataResponse.from(null));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
