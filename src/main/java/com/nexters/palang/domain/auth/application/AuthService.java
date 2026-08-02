package com.nexters.palang.domain.auth.application;

import com.nexters.palang.domain.auth.domain.RefreshToken;
import com.nexters.palang.domain.auth.infrastructure.KakaoOAuthClient;
import com.nexters.palang.domain.auth.infrastructure.RefreshTokenRepository;
import com.nexters.palang.domain.user.application.UserRegistrationService;
import com.nexters.palang.domain.user.common.error.UserErrorCode;
import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import com.nexters.palang.global.security.AuthErrorCode;
import com.nexters.palang.global.security.AuthException;
import com.nexters.palang.global.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final UserRegistrationService userRegistrationService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResult loginWithKakao(String kakaoAccessToken) {
        KakaoOAuthClient.KakaoUserInfo kakaoUserInfo = kakaoOAuthClient.getUserInfo(kakaoAccessToken);
        User user = userRepository.findBySnsProviderAndSnsId(SnsProvider.KAKAO, kakaoUserInfo.snsId()).orElse(null);

        boolean isNewUser = user == null;
        if (isNewUser) {
            user = userRegistrationService.registerViaSns(SnsProvider.KAKAO, kakaoUserInfo.snsId(), kakaoUserInfo.email());
        } else if (user.isWithdrawn()) {
            throw new AuthException(AuthErrorCode.WITHDRAWN_ACCOUNT);
        } else if (kakaoUserInfo.email() != null) {
            // 최초 로그인 시 이메일 동의를 거부했다가 나중에 동의하는 경우를 위해 갱신한다.
            user.changeEmail(kakaoUserInfo.email());
        }

        return issueTokens(user, isNewUser);
    }

    @Transactional
    public void agreeToTerms(Long userId) {
        getActiveUser(userId).agreeToTerms();
    }

    // local 프로파일 전용 개발 편의 기능(DevAuthController). 카카오 서버를 타지 않고 바로 JWT를 발급한다.
    // userId가 주어지면 그 유저로, 없으면 매번 새 테스트 유저를 만들어 로그인 처리한다.
    @Transactional
    public AuthResult devLogin(Long userId) {
        boolean isNewUser = userId == null;
        User user = isNewUser
                ? userRegistrationService.registerViaSns(SnsProvider.KAKAO, "dev-" + System.nanoTime(), null)
                : getActiveUser(userId);
        return issueTokens(user, isNewUser);
    }

    @Transactional
    public AuthResult refresh(String refreshToken) {
        Long userId = parseUserId(refreshToken);
        String tokenHash = hash(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByUserIdAndTokenHashAndRevokedFalse(userId, tokenHash)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));
        if (stored.isExpired()) {
            throw new AuthException(AuthErrorCode.TOKEN_EXPIRED);
        }
        stored.revoke();

        return issueTokens(getActiveUser(userId), false);
    }

    @Transactional
    public void logout(Long userId, String refreshToken) {
        refreshTokenRepository.findByUserIdAndTokenHash(userId, hash(refreshToken))
                .ifPresent(RefreshToken::revoke);
    }

    private AuthResult issueTokens(User user, boolean isNewUser) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        RefreshToken token = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hash(refreshToken))
                .expiresAt(jwtTokenProvider.refreshTokenExpiryFromNow())
                .build();
        refreshTokenRepository.save(token);

        return new AuthResult(
                accessToken, refreshToken, isNewUser,
                user.getTermsAgreedAt() != null, user.isHasCompletedOnboarding());
    }

    private Long parseUserId(String token) {
        try {
            return jwtTokenProvider.getUserId(token);
        } catch (ExpiredJwtException e) {
            throw new AuthException(AuthErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    // 탈퇴한 사용자는 더 이상 조작 대상이 아니므로 존재하지 않는 것과 동일하게 취급한다(UserService와 동일한 정책).
    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        if (user.isWithdrawn()) {
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    // 원문 리프레시 토큰을 DB에 그대로 저장하지 않기 위한 SHA-256 해시.
    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
