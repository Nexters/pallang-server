package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.application.AdminAccessGuard;
import com.nexters.palang.domain.admin.application.AdminUserMapper;
import com.nexters.palang.domain.admin.application.AdminUserSearchResult;
import com.nexters.palang.domain.admin.application.AdminUserService;
import com.nexters.palang.domain.admin.presentation.dto.AdminUserListResponse;
import com.nexters.palang.global.common.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminUserController implements AdminUserApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AdminAccessGuard adminAccessGuard;
    private final AdminUserService adminUserService;

    @Override
    @GetMapping("/api/admin/users")
    public ResponseEntity<DataResponse<AdminUserListResponse>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        adminAccessGuard.requireAdmin();
        Page<AdminUserSearchResult> results = adminUserService.searchUsers(keyword, pageable(page, size));
        return ResponseEntity.ok(DataResponse.from(AdminUserMapper.toListResponse(results)));
    }

    @Override
    @DeleteMapping("/api/admin/users/{userId}")
    public ResponseEntity<DataResponse<Void>> deleteUser(@PathVariable Long userId) {
        adminAccessGuard.requireAdmin();
        adminUserService.deleteUser(userId);
        return ResponseEntity.ok(DataResponse.from(null));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
