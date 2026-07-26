package com.nexters.palang.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.global.security.AuthErrorCode;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-jwt-secret-key-for-unit-test-01234567890123456789";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 3600, 1209600);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 토큰이 있으면 request attribute와 SecurityContext에 유저 ID가 채워진다")
    void setsUserIdWhenTokenValid() throws Exception {
        String token = jwtTokenProvider.createAccessToken(1L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute(JwtAuthenticationFilter.CURRENT_USER_ID_ATTRIBUTE)).isEqualTo(1L);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    @DisplayName("토큰이 없으면 아무 attribute도 채우지 않고 통과시킨다")
    void passesThroughWhenNoToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute(JwtAuthenticationFilter.CURRENT_USER_ID_ATTRIBUTE)).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE)).isNull();
    }

    @Test
    @DisplayName("만료된 토큰이면 TOKEN_EXPIRED 에러를 attribute에 남긴다")
    void marksExpiredTokenError() throws Exception {
        JwtTokenProvider expiringProvider = new JwtTokenProvider(SECRET, -1, -1);
        JwtAuthenticationFilter expiringFilter = new JwtAuthenticationFilter(expiringProvider);
        String expiredToken = expiringProvider.createAccessToken(1L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + expiredToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        expiringFilter.doFilter(request, response, chain);

        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE))
                .isEqualTo(AuthErrorCode.TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("형식이 올바르지 않은 토큰이면 INVALID_TOKEN 에러를 attribute에 남긴다")
    void marksInvalidTokenError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE))
                .isEqualTo(AuthErrorCode.INVALID_TOKEN);
    }
}
