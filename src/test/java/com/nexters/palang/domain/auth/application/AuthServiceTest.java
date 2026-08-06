package com.nexters.palang.domain.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.auth.domain.RefreshToken;
import com.nexters.palang.domain.auth.infrastructure.AppleAuthClient;
import com.nexters.palang.domain.auth.infrastructure.AppleOAuthClient;
import com.nexters.palang.domain.auth.infrastructure.KakaoOAuthClient;
import com.nexters.palang.domain.auth.infrastructure.RefreshTokenRepository;
import com.nexters.palang.domain.user.application.UserRegistrationService;
import com.nexters.palang.domain.user.application.UserService;
import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import com.nexters.palang.global.security.AuthErrorCode;
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
    private UserService userService;

    @Mock
    private UserRegistrationService userRegistrationService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;

    @Mock
    private AppleOAuthClient appleOAuthClient;

    @Mock
    private AppleAuthClient appleAuthClient;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, userService, userRegistrationService, refreshTokenRepository,
                kakaoOAuthClient, appleOAuthClient, appleAuthClient, jwtTokenProvider);
    }

    private User user(Long id) {
        return user(id, SnsProvider.KAKAO);
    }

    private User user(Long id, SnsProvider snsProvider) {
        User user = User.builder().nickname("닉네임" + id).snsProvider(snsProvider).snsId("sns-" + id).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("처음 로그인하는 카카오 사용자는 신규 가입되고 isNewUser가 true다")
    void loginWithKakaoSignsUpNewUser() {
        given(kakaoOAuthClient.getUserInfo("kakao-token"))
                .willReturn(new KakaoOAuthClient.KakaoUserInfo("sns-100", "user100@example.com"));
        given(userRepository.findBySnsProviderAndSnsId(SnsProvider.KAKAO, "sns-100")).willReturn(Optional.empty());
        User newUser = user(100L);
        given(userRegistrationService.registerViaSns(SnsProvider.KAKAO, "sns-100", "user100@example.com", null))
                .willReturn(newUser);
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
    @DisplayName("이미 가입된 카카오 사용자가 로그인하면 isNewUser가 false이고 이메일이 갱신된다")
    void loginWithKakaoSignsInExistingUser() {
        given(kakaoOAuthClient.getUserInfo("kakao-token"))
                .willReturn(new KakaoOAuthClient.KakaoUserInfo("sns-200", "user200@example.com"));
        User existingUser = user(200L);
        given(userRepository.findBySnsProviderAndSnsId(SnsProvider.KAKAO, "sns-200"))
                .willReturn(Optional.of(existingUser));
        given(jwtTokenProvider.createAccessToken(200L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(200L)).willReturn("refresh-token");
        given(jwtTokenProvider.refreshTokenExpiryFromNow()).willReturn(LocalDateTime.now().plusDays(14));

        AuthResult result = authService.loginWithKakao("kakao-token");

        assertThat(result.isNewUser()).isFalse();
        assertThat(existingUser.getEmail()).isEqualTo("user200@example.com");
        verify(userRegistrationService, never()).registerViaSns(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("탈퇴한 계정으로 같은 카카오 계정으로 재로그인하면 차단되지 않고 재가입 처리된다")
    void loginWithKakaoReactivatesWithdrawnAccount() {
        given(kakaoOAuthClient.getUserInfo("kakao-token"))
                .willReturn(new KakaoOAuthClient.KakaoUserInfo("sns-300", "user300@example.com"));
        User withdrawnUser = user(300L);
        withdrawnUser.withdraw();
        given(userRepository.findBySnsProviderAndSnsId(SnsProvider.KAKAO, "sns-300"))
                .willReturn(Optional.of(withdrawnUser));
        given(userRegistrationService.reactivate(withdrawnUser, "user300@example.com", null))
                .willReturn(withdrawnUser);
        given(jwtTokenProvider.createAccessToken(300L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(300L)).willReturn("refresh-token");
        given(jwtTokenProvider.refreshTokenExpiryFromNow()).willReturn(LocalDateTime.now().plusDays(14));

        AuthResult result = authService.loginWithKakao("kakao-token");

        assertThat(result.isNewUser()).isTrue();
        verify(userRegistrationService).reactivate(withdrawnUser, "user300@example.com", null);
    }

    @Test
    @DisplayName("처음 로그인하는 애플 사용자는 신규 가입되고 isNewUser가 true다")
    void loginWithAppleSignsUpNewUser() {
        given(appleOAuthClient.getUserInfo("apple-token"))
                .willReturn(new AppleOAuthClient.AppleUserInfo("apple-sns-100", "apple100@example.com", "com.palang.app"));
        given(userRepository.findBySnsProviderAndSnsId(SnsProvider.APPLE, "apple-sns-100")).willReturn(Optional.empty());
        User newUser = user(100L, SnsProvider.APPLE);
        given(userRegistrationService.registerViaSns(SnsProvider.APPLE, "apple-sns-100", "apple100@example.com", null))
                .willReturn(newUser);
        given(jwtTokenProvider.createAccessToken(100L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(100L)).willReturn("refresh-token");
        given(jwtTokenProvider.refreshTokenExpiryFromNow()).willReturn(LocalDateTime.now().plusDays(14));

        AuthResult result = authService.loginWithApple("apple-token", null, null, null);

        assertThat(result.isNewUser()).isTrue();
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("최초 로그인 시 애플이 이름을 내려주면 성+이름 순서로 합쳐 저장한다")
    void loginWithAppleCombinesGivenAndFamilyNameOnSignUp() {
        given(appleOAuthClient.getUserInfo("apple-token"))
                .willReturn(new AppleOAuthClient.AppleUserInfo("apple-sns-101", null, "com.palang.app"));
        given(userRepository.findBySnsProviderAndSnsId(SnsProvider.APPLE, "apple-sns-101")).willReturn(Optional.empty());
        User newUser = user(101L, SnsProvider.APPLE);
        given(userRegistrationService.registerViaSns(SnsProvider.APPLE, "apple-sns-101", null, "홍길동"))
                .willReturn(newUser);
        given(jwtTokenProvider.createAccessToken(101L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(101L)).willReturn("refresh-token");
        given(jwtTokenProvider.refreshTokenExpiryFromNow()).willReturn(LocalDateTime.now().plusDays(14));

        authService.loginWithApple("apple-token", null, "길동", "홍");

        verify(userRegistrationService).registerViaSns(SnsProvider.APPLE, "apple-sns-101", null, "홍길동");
    }

    @Test
    @DisplayName("authorizationCode가 있으면 애플에 교환해 refresh token을 저장한다")
    void loginWithAppleStoresAppleRefreshTokenWhenAuthorizationCodeGiven() {
        given(appleOAuthClient.getUserInfo("apple-token"))
                .willReturn(new AppleOAuthClient.AppleUserInfo("apple-sns-102", null, "com.palang.app"));
        User existingUser = user(102L, SnsProvider.APPLE);
        given(userRepository.findBySnsProviderAndSnsId(SnsProvider.APPLE, "apple-sns-102"))
                .willReturn(Optional.of(existingUser));
        given(appleAuthClient.exchangeForRefreshToken("auth-code", "com.palang.app"))
                .willReturn(Optional.of("apple-refresh-token"));
        given(jwtTokenProvider.createAccessToken(102L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(102L)).willReturn("refresh-token");
        given(jwtTokenProvider.refreshTokenExpiryFromNow()).willReturn(LocalDateTime.now().plusDays(14));

        authService.loginWithApple("apple-token", "auth-code", null, null);

        assertThat(existingUser.getAppleRefreshToken()).isEqualTo("apple-refresh-token");
    }

    @Test
    @DisplayName("authorizationCode 교환에 실패해도 로그인은 계속 진행된다")
    void loginWithAppleSucceedsEvenWhenAuthorizationCodeExchangeFails() {
        given(appleOAuthClient.getUserInfo("apple-token"))
                .willReturn(new AppleOAuthClient.AppleUserInfo("apple-sns-103", null, "com.palang.app"));
        User existingUser = user(103L, SnsProvider.APPLE);
        given(userRepository.findBySnsProviderAndSnsId(SnsProvider.APPLE, "apple-sns-103"))
                .willReturn(Optional.of(existingUser));
        given(appleAuthClient.exchangeForRefreshToken("auth-code", "com.palang.app")).willReturn(Optional.empty());
        given(jwtTokenProvider.createAccessToken(103L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(103L)).willReturn("refresh-token");
        given(jwtTokenProvider.refreshTokenExpiryFromNow()).willReturn(LocalDateTime.now().plusDays(14));

        AuthResult result = authService.loginWithApple("apple-token", "auth-code", null, null);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(existingUser.getAppleRefreshToken()).isNull();
    }

    @Test
    @DisplayName("이미 가입된 애플 사용자가 로그인하면 isNewUser가 false이고 이메일이 갱신된다")
    void loginWithAppleSignsInExistingUser() {
        given(appleOAuthClient.getUserInfo("apple-token"))
                .willReturn(new AppleOAuthClient.AppleUserInfo("apple-sns-200", "apple200@example.com", "com.palang.app"));
        User existingUser = user(200L, SnsProvider.APPLE);
        given(userRepository.findBySnsProviderAndSnsId(SnsProvider.APPLE, "apple-sns-200"))
                .willReturn(Optional.of(existingUser));
        given(jwtTokenProvider.createAccessToken(200L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(200L)).willReturn("refresh-token");
        given(jwtTokenProvider.refreshTokenExpiryFromNow()).willReturn(LocalDateTime.now().plusDays(14));

        AuthResult result = authService.loginWithApple("apple-token", null, null, null);

        assertThat(result.isNewUser()).isFalse();
        assertThat(existingUser.getEmail()).isEqualTo("apple200@example.com");
        verify(userRegistrationService, never()).registerViaSns(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("탈퇴한 계정으로 같은 애플 계정으로 재로그인하면 차단되지 않고 재가입 처리된다")
    void loginWithAppleReactivatesWithdrawnAccount() {
        given(appleOAuthClient.getUserInfo("apple-token"))
                .willReturn(new AppleOAuthClient.AppleUserInfo("apple-sns-300", null, "com.palang.app"));
        User withdrawnUser = user(300L, SnsProvider.APPLE);
        withdrawnUser.withdraw();
        given(userRepository.findBySnsProviderAndSnsId(SnsProvider.APPLE, "apple-sns-300"))
                .willReturn(Optional.of(withdrawnUser));
        given(userRegistrationService.reactivate(withdrawnUser, null, null)).willReturn(withdrawnUser);
        given(jwtTokenProvider.createAccessToken(300L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(300L)).willReturn("refresh-token");
        given(jwtTokenProvider.refreshTokenExpiryFromNow()).willReturn(LocalDateTime.now().plusDays(14));

        AuthResult result = authService.loginWithApple("apple-token", null, null, null);

        assertThat(result.isNewUser()).isTrue();
        verify(userRegistrationService).reactivate(withdrawnUser, null, null);
    }

    @Test
    @DisplayName("[개발용] userId 없이 devLogin을 호출하면 새 테스트 유저를 만들어 로그인 처리한다")
    void devLoginCreatesNewUserWhenUserIdOmitted() {
        User newUser = user(400L);
        given(userRegistrationService.registerViaSns(eq(SnsProvider.KAKAO), anyString(), any(), any())).willReturn(newUser);
        given(jwtTokenProvider.createAccessToken(400L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(400L)).willReturn("refresh-token");
        given(jwtTokenProvider.refreshTokenExpiryFromNow()).willReturn(LocalDateTime.now().plusDays(14));

        AuthResult result = authService.devLogin(null);

        assertThat(result.isNewUser()).isTrue();
        assertThat(result.accessToken()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("[개발용] userId를 주고 devLogin을 호출하면 해당 유저로 로그인 처리한다")
    void devLoginUsesExistingUserWhenUserIdGiven() {
        User existingUser = user(500L);
        given(userRepository.findById(500L)).willReturn(Optional.of(existingUser));
        given(jwtTokenProvider.createAccessToken(500L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(500L)).willReturn("refresh-token");
        given(jwtTokenProvider.refreshTokenExpiryFromNow()).willReturn(LocalDateTime.now().plusDays(14));

        AuthResult result = authService.devLogin(500L);

        assertThat(result.isNewUser()).isFalse();
        verify(userRegistrationService, never()).registerViaSns(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("[개발용] 존재하지 않는 userId로 devLogin을 호출하면 예외가 발생한다")
    void devLoginFailsWhenUserIdNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.devLogin(999L)).isInstanceOf(UserException.class);
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

    @Test
    @DisplayName("카카오 사용자가 탈퇴하면 카카오 연동 해제와 리프레시 토큰 일괄 폐기, 소프트 삭제가 모두 이뤄진다")
    void withdrawUnlinksKakaoAndRevokesTokens() {
        User user = user(1L, SnsProvider.KAKAO);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        authService.withdraw(1L);

        verify(kakaoOAuthClient).unlink("sns-1");
        verify(refreshTokenRepository).revokeAllByUserId(1L);
        verify(userService).withdraw(1L);
    }

    @Test
    @DisplayName("애플 사용자가 탈퇴하면 카카오 연동 해제는 호출되지 않는다")
    void withdrawDoesNotUnlinkKakaoForAppleUser() {
        User user = user(1L, SnsProvider.APPLE);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        authService.withdraw(1L);

        verify(kakaoOAuthClient, never()).unlink(anyString());
        verify(refreshTokenRepository).revokeAllByUserId(1L);
        verify(userService).withdraw(1L);
    }

    @Test
    @DisplayName("애플 사용자가 탈퇴하면 저장된 refresh token으로 애플 연동을 해제한다")
    void withdrawRevokesAppleWithStoredRefreshToken() {
        User user = user(1L, SnsProvider.APPLE);
        user.updateAppleRefreshToken("apple-refresh-token");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        authService.withdraw(1L);

        verify(appleAuthClient).revoke("apple-refresh-token");
        verify(refreshTokenRepository).revokeAllByUserId(1L);
        verify(userService).withdraw(1L);
    }

    @Test
    @DisplayName("애플 refresh token이 없으면 연동 해제를 시도하지 않고도 탈퇴는 진행된다")
    void withdrawSkipsAppleRevokeWhenRefreshTokenMissing() {
        User user = user(1L, SnsProvider.APPLE);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        authService.withdraw(1L);

        verify(appleAuthClient, never()).revoke(anyString());
        verify(refreshTokenRepository).revokeAllByUserId(1L);
        verify(userService).withdraw(1L);
    }

    @Test
    @DisplayName("애플 연동 해제가 실패해도 탈퇴 자체는 계속 진행된다")
    void withdrawContinuesEvenWhenAppleRevokeFails() {
        User user = user(1L, SnsProvider.APPLE);
        user.updateAppleRefreshToken("apple-refresh-token");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        doThrow(new AuthException(AuthErrorCode.APPLE_REVOKE_FAILED))
                .when(appleAuthClient).revoke("apple-refresh-token");

        authService.withdraw(1L);

        verify(refreshTokenRepository).revokeAllByUserId(1L);
        verify(userService).withdraw(1L);
    }

    @Test
    @DisplayName("카카오 연동 해제가 실패해도 탈퇴 자체는 계속 진행된다")
    void withdrawContinuesEvenWhenKakaoUnlinkFails() {
        User user = user(1L, SnsProvider.KAKAO);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        doThrow(new AuthException(AuthErrorCode.KAKAO_UNLINK_FAILED)).when(kakaoOAuthClient).unlink("sns-1");

        authService.withdraw(1L);

        verify(refreshTokenRepository).revokeAllByUserId(1L);
        verify(userService).withdraw(1L);
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 탈퇴하려 하면 예외가 발생한다")
    void withdrawFailsWhenUserNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.withdraw(999L)).isInstanceOf(UserException.class);
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
