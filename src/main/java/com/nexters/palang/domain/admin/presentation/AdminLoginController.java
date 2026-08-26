package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.application.AdminLoginService;
import com.nexters.palang.domain.admin.presentation.dto.AdminLoginRequest;
import com.nexters.palang.domain.admin.presentation.dto.AdminLoginResponse;
import com.nexters.palang.global.common.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminLoginController implements AdminLoginApi {

    private final AdminLoginService adminLoginService;

    @Override
    @PostMapping("/api/admin/auth/login")
    public ResponseEntity<DataResponse<AdminLoginResponse>> login(@Valid @RequestBody AdminLoginRequest request) {
        String token = adminLoginService.login(request.username(), request.password());
        return ResponseEntity.ok(DataResponse.from(new AdminLoginResponse(token)));
    }
}
