package com.nexters.palang.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.global.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class JwtCurrentUserProviderTest {

    @Test
    @DisplayName("request attribute에 유저 ID가 있으면 그대로 반환한다")
    void returnsUserIdFromAttribute() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(JwtAuthenticationFilter.CURRENT_USER_ID_ATTRIBUTE, 1L);
        JwtCurrentUserProvider provider = new JwtCurrentUserProvider(request);

        assertThat(provider.getCurrentUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("토큰이 만료/유효하지 않아 에러가 채워져 있으면 그 에러로 예외가 발생한다")
    void throwsAuthExceptionWhenAuthErrorPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE, AuthErrorCode.TOKEN_EXPIRED);
        JwtCurrentUserProvider provider = new JwtCurrentUserProvider(request);

        assertThatThrownBy(provider::getCurrentUserId)
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("토큰이 아예 없으면 로그인 필요 예외가 발생한다")
    void throwsLoginRequiredWhenNothingPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        JwtCurrentUserProvider provider = new JwtCurrentUserProvider(request);

        assertThatThrownBy(provider::getCurrentUserId).isInstanceOf(LoginRequiredException.class);
    }
}
