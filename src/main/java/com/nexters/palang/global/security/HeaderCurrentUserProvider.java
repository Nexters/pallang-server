package com.nexters.palang.global.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 인증(JWT) Phase 착수 전까지 사용하는 임시 인증 스탠드인.
 * X-Debug-User-Id 요청 헤더 값을 그대로 유저 ID로 사용한다.
 * 인증 Phase 착수 시 JwtCurrentUserProvider로 구현체만 교체한다.
 */
@Profile("local")
@Component
public class HeaderCurrentUserProvider implements CurrentUserProvider {

    private static final String DEBUG_USER_ID_HEADER = "X-Debug-User-Id";

    private final HttpServletRequest request;

    public HeaderCurrentUserProvider(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public Long getCurrentUserId() {
        String userId = request.getHeader(DEBUG_USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            throw new LoginRequiredException();
        }
        return Long.valueOf(userId);
    }
}
