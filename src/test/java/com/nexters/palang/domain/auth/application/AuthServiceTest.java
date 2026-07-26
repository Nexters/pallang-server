package com.nexters.palang.domain.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.auth.domain.RefreshToken;
import com.nexters.palang.domain.auth.infrastructure.KakaoOAuthClient;
import com.nexters.palang.domain.auth.infrastructure.RefreshTokenRepository;
import com.nexters.palang.domain.user.application.UserRegistrationService;
import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import com.nexters.palang.global.security.AuthException;
import com.nexters.palang.global.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRegistrationService userRegistrationService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, userRegistrationService, refreshTokenRepository, kakaoOAuthClient, jwtTokenProvider);
    }

    private User user(Long id) {
        User user = User.builder().nickname("닉네임" + id).snsProvider(SnsProvider.KAKAO).snsId("sns-" + id).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("처음 로그인하는 카카오 사용자는 신규 가입되고 isNewUser가 true다")
    void loginWithKakaoSignsUpNewUser() {
        given(kakaoOAuthClient.getSnsId("kakao-token")).willReturn("sns-100");
        given(userRepository.findBySnsProviderAndSnsId(SnsProvider.KAKAO, "sns-100")).willReturn(Optional.empty());
        User newUser = user(100L);
        given(userRegistrationService.registerViaSns(SnsProvider.KAKAO, "sns-100")).willReturn(newUser);
        given(jwtTokenProvider.createAccessToken(100L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(100L)).willReturn("refresh-token");
        given(jwtTokenProvider.refreshTokenExpiryFromNow()).willReturn(LocalDateTime.now().plusDays(14));

        AuthResult result = authService.loginWithKakao("kakao-token");

        assertThat(result.isNewUser()).isTrue();
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.termsAgreed()).isFalse();
        assertThat(result.hasCompletedOnboarding()).isFalse();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("이미 가입된 카카오 사용자가 로그인하면 isNewUser가 false다")
    void loginWithKakaoSignsInExistingUser() {
        given(kakaoOAuthClient.getSnsId("kakao-token")).willReturn("sns-200");
        User existingUser = user(200L);
        given(userRepository.findBySnsProviderAndSnsId(SnsProvider.KAKAO, "sns-200"))
                .willReturn(Optional.of(existingUser));
        given(jwtTokenProvider.createAccessToken(200L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(200L)).willReturn("refresh-token");
        given(jwtTokenProvider.refreshTokenExpiryFromNow()).willReturn(LocalDateTime.now().plusDays(14));

        AuthResult result = authService.loginWithKakao("kakao-token");

        assertThat(result.isNewUser()).isFalse();
        verify(userRegistrationService, never()).registerViaSns(any(), anyString());
    }

    @Test
    @DisplayName("탈퇴한 계정으로 로그인을 시도하면 예외가 발생한다")
    void loginWithKakaoFailsWhenAccountWithdrawn() {
        given(kakaoOAuthClient.getSnsId("kakao-token")).willReturn("sns-300");
        User withdrawnUser = user(300L);
        withdrawnUser.withdraw();
        given(userRepository.findBySnsProviderAndSnsId(SnsProvider.KAKAO, "sns-300"))
                .willReturn(Optional.of(withdrawnUser));

        assertThatThrownBy(() -> authService.loginWithKakao("kakao-token")).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("약관에 동의하면 사용자의 동의 시각이 기록된다")
    void agreeToTermsUpdatesUser() {
        User user = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        authService.agreeToTerms(1L);

        assertThat(user.getTermsAgreedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 약관에 동의하려 하면 예외가 발생한다")
    void agreeToTermsFailsWhenUserNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.agreeToTerms(999L)).isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 재발급하면 기존 토큰은 폐기되고 새 토큰이 발급된다")
    void refreshIssuesNewTokensAndRevokesOld() {
        given(jwtTokenProvider.getUserId("old-refresh")).willReturn(1L);
        RefreshToken stored = RefreshToken.builder()
                .userId(1L).tokenHash(sha256("old-refresh")).expiresAt(LocalDateTime.now().plusDays(1)).build();
        given(refreshTokenRepository.findByUserIdAndTokenHashAndRevokedFalse(1L, sha256("old-refresh")))
                .willReturn(Optional.of(stored));
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("new-access");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("new-refresh");
        given(jwtTokenProvider.refreshTokenExpiryFromNow()).willReturn(LocalDateTime.now().plusDays(14));

        AuthResult result = authService.refresh("old-refresh");

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("만료된 액세스 토큰이 아니라 만료된 리프레시 토큰으로 재발급을 시도하면 예외가 발생한다")
    void refreshFailsWhenStoredTokenExpired() {
        given(jwtTokenProvider.getUserId("old-refresh")).willReturn(1L);
        RefreshToken stored = RefreshToken.builder()
                .userId(1L).tokenHash(sha256("old-refresh")).expiresAt(LocalDateTime.now().minusDays(1)).build();
        given(refreshTokenRepository.findByUserIdAndTokenHashAndRevokedFalse(1L, sha256("old-refresh")))
                .willReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("old-refresh")).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("저장된 적 없는(또는 이미 폐기된) 리프레시 토큰으로 재발급을 시도하면 예외가 발생한다")
    void refreshFailsWhenTokenNotFound() {
        given(jwtTokenProvider.getUserId("unknown-refresh")).willReturn(1L);
        given(refreshTokenRepository.findByUserIdAndTokenHashAndRevokedFalse(anyLong(), anyString()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("unknown-refresh")).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("만료된 JWT 형식의 리프레시 토큰이면 토큰 만료 예외가 발생한다")
    void refreshFailsWhenTokenExpiredAtParseTime() {
        given(jwtTokenProvider.getUserId("expired-jwt")).willThrow(new ExpiredJwtException(null, null, "expired"));

        assertThatThrownBy(() -> authService.refresh("expired-jwt")).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("형식이 올바르지 않은 리프레시 토큰이면 유효하지 않은 토큰 예외가 발생한다")
    void refreshFailsWhenTokenMalformed() {
        given(jwtTokenProvider.getUserId("malformed")).willThrow(new MalformedJwtException("malformed"));

        assertThatThrownBy(() -> authService.refresh("malformed")).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("로그아웃하면 해당 리프레시 토큰이 폐기된다")
    void logoutRevokesRefreshToken() {
        RefreshToken stored = RefreshToken.builder()
                .userId(1L).tokenHash(sha256("refresh")).expiresAt(LocalDateTime.now().plusDays(1)).build();
        given(refreshTokenRepository.findByUserIdAndTokenHash(1L, sha256("refresh"))).willReturn(Optional.of(stored));

        authService.logout(1L, "refresh");

        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 리프레시 토큰으로 로그아웃해도 에러 없이 무시된다")
    void logoutIsNoOpWhenTokenNotFound() {
        given(refreshTokenRepository.findByUserIdAndTokenHash(1L, sha256("refresh"))).willReturn(Optional.empty());

        authService.logout(1L, "refresh");
    }

    private String sha256(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hashBytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
