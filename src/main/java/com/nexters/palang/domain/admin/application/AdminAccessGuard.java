package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.admin.common.error.AdminErrorCode;
import com.nexters.palang.domain.admin.common.error.AdminException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 관리자 페이지/API는 일반 유저 로그인(JWT+CurrentUserProvider)과 완전히 분리된 별도 로그인
// (AdminLoginService, POST /api/admin/auth/login)을 쓴다. 이 가드는 Authorization 헤더의 토큰을
// AdminJwtProvider로 직접 검증한다 — 특정 유저 계정에 종속되지 않는, 이 페이지만을 위한 자격 증명이다.
@Component
@RequiredArgsConstructor
public class AdminAccessGuard {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final HttpServletRequest request;
    private final AdminJwtProvider adminJwtProvider;

    public void requireAdmin() {
        String token = resolveToken();
        if (token == null || !adminJwtProvider.isValid(token)) {
            throw new AdminException(AdminErrorCode.ADMIN_ACCESS_DENIED);
        }
    }

    private String resolveToken() {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
