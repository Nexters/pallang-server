package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.application.AdminAccessGuard;
import com.nexters.palang.domain.admin.application.AdminGroupMapper;
import com.nexters.palang.domain.admin.application.AdminGroupSearchResult;
import com.nexters.palang.domain.admin.application.AdminGroupService;
import com.nexters.palang.domain.admin.presentation.dto.AdminGroupListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminGroupSummaryResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminUpdateGroupRequest;
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
public class AdminGroupController implements AdminGroupApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AdminAccessGuard adminAccessGuard;
    private final AdminGroupService adminGroupService;

    @Override
    @GetMapping("/api/admin/groups")
    public ResponseEntity<DataResponse<AdminGroupListResponse>> searchGroups(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        adminAccessGuard.requireAdmin();
        Page<AdminGroupSearchResult> results = adminGroupService.searchGroups(keyword, pageable(page, size));
        return ResponseEntity.ok(DataResponse.from(AdminGroupMapper.toListResponse(results)));
    }

    @Override
    @PatchMapping("/api/admin/groups/{groupId}")
    public ResponseEntity<DataResponse<AdminGroupSummaryResponse>> updateGroup(
            @PathVariable Long groupId, @Valid @RequestBody AdminUpdateGroupRequest request) {
        adminAccessGuard.requireAdmin();
        AdminGroupSearchResult result = adminGroupService.updateGroup(
                groupId, request.name(), request.capacity(), request.startDate(), request.endDate());
        return ResponseEntity.ok(DataResponse.from(AdminGroupMapper.toSummary(result)));
    }

    @Override
    @DeleteMapping("/api/admin/groups/{groupId}")
    public ResponseEntity<DataResponse<Void>> deleteGroup(@PathVariable Long groupId) {
        adminAccessGuard.requireAdmin();
        adminGroupService.deleteGroup(groupId);
        return ResponseEntity.ok(DataResponse.from(null));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
