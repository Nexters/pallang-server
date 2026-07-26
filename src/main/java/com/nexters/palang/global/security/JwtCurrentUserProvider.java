package com.nexters.palang.global.security;

import com.nexters.palang.global.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 인증 Phase의 실제 구현체. JwtAuthenticationFilter가 request attribute에 채워둔 유저 ID를 그대로 꺼낸다.
 * HeaderCurrentUserProvider(local 프로파일 임시 스탠드인)보다 우선하도록 @Primary로 지정한다.
 */
@Primary
@Component
@RequiredArgsConstructor
public class JwtCurrentUserProvider implements CurrentUserProvider {

    private final HttpServletRequest request;

    @Override
    public Long getCurrentUserId() {
        Object userId = request.getAttribute(JwtAuthenticationFilter.CURRENT_USER_ID_ATTRIBUTE);
        if (userId != null) {
            return (Long) userId;
        }
        Object authError = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        if (authError instanceof AuthErrorCode errorCode) {
            throw new AuthException(errorCode);
        }
        throw new LoginRequiredException();
    }
}
