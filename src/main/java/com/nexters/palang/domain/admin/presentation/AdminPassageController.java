package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.application.AdminAccessGuard;
import com.nexters.palang.domain.admin.application.AdminPassageMapper;
import com.nexters.palang.domain.admin.application.AdminPassageService;
import com.nexters.palang.domain.admin.presentation.dto.AdminPassageListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminPassageSummaryResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminUpdatePassageRequest;
import com.nexters.palang.domain.passage.domain.Passage;
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
public class AdminPassageController implements AdminPassageApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AdminAccessGuard adminAccessGuard;
    private final AdminPassageService adminPassageService;

    @Override
    @GetMapping("/api/admin/passages")
    public ResponseEntity<DataResponse<AdminPassageListResponse>> searchPassages(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        adminAccessGuard.requireAdmin();
        Page<Passage> results = adminPassageService.searchPassages(keyword, pageable(page, size));
        return ResponseEntity.ok(DataResponse.from(AdminPassageMapper.toListResponse(results)));
    }

    @Override
    @PatchMapping("/api/admin/passages/{passageId}")
    public ResponseEntity<DataResponse<AdminPassageSummaryResponse>> updatePassage(
            @PathVariable Long passageId, @Valid @RequestBody AdminUpdatePassageRequest request) {
        adminAccessGuard.requireAdmin();
        Passage passage = adminPassageService.updatePassage(
                passageId, request.quotedText(), request.pageNumber(), request.isSpoiler());
        return ResponseEntity.ok(DataResponse.from(AdminPassageMapper.toSummary(passage)));
    }

    @Override
    @DeleteMapping("/api/admin/passages/{passageId}")
    public ResponseEntity<DataResponse<Void>> deletePassage(@PathVariable Long passageId) {
        adminAccessGuard.requireAdmin();
        adminPassageService.deletePassage(passageId);
        return ResponseEntity.ok(DataResponse.from(null));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
