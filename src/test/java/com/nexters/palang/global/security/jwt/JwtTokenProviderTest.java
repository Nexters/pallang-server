package com.nexters.palang.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "test-jwt-secret-key-for-unit-test-01234567890123456789";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 3600, 1209600);

    @Test
    @DisplayName("액세스 토큰을 발급하면 같은 유저 ID로 파싱된다")
    void createAccessTokenRoundTrips() {
        String token = jwtTokenProvider.createAccessToken(1L);

        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(1L);
    }

    @Test
    @DisplayName("리프레시 토큰을 발급하면 같은 유저 ID로 파싱된다")
    void createRefreshTokenRoundTrips() {
        String token = jwtTokenProvider.createRefreshToken(42L);

        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("만료된 토큰을 파싱하면 ExpiredJwtException이 발생한다")
    void getUserIdFailsWhenTokenExpired() {
        JwtTokenProvider expiringProvider = new JwtTokenProvider(SECRET, -1, -1);
        String expiredToken = expiringProvider.createAccessToken(1L);

        assertThatThrownBy(() -> expiringProvider.getUserId(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("형식이 올바르지 않은 토큰을 파싱하면 JwtException이 발생한다")
    void getUserIdFailsWhenTokenMalformed() {
        assertThatThrownBy(() -> jwtTokenProvider.getUserId("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("리프레시 토큰 만료 시각은 현재 시각보다 미래다")
    void refreshTokenExpiryFromNowIsInFuture() {
        assertThat(jwtTokenProvider.refreshTokenExpiryFromNow()).isAfter(java.time.LocalDateTime.now());
    }
}
