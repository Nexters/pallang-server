package com.nexters.palang.domain.auth.presentation;

import com.nexters.palang.domain.auth.application.AuthMapper;
import com.nexters.palang.domain.auth.application.AuthResult;
import com.nexters.palang.domain.auth.application.AuthService;
import com.nexters.palang.domain.auth.presentation.dto.KakaoLoginRequest;
import com.nexters.palang.domain.auth.presentation.dto.LoginResponse;
import com.nexters.palang.domain.auth.presentation.dto.RefreshTokenRequest;
import com.nexters.palang.domain.auth.presentation.dto.TermsAgreementResponse;
import com.nexters.palang.domain.auth.presentation.dto.TokenResponse;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @PostMapping("/api/auth/kakao")
    public ResponseEntity<DataResponse<LoginResponse>> loginWithKakao(@RequestBody @Valid KakaoLoginRequest request) {
        AuthResult result = authService.loginWithKakao(request.kakaoAccessToken());
        return ResponseEntity.ok(DataResponse.from(AuthMapper.toLoginResponse(result)));
    }

    @Override
    @PostMapping("/api/auth/terms")
    public ResponseEntity<DataResponse<TermsAgreementResponse>> agreeToTerms() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        authService.agreeToTerms(currentUserId);
        return ResponseEntity.ok(DataResponse.from(new TermsAgreementResponse(true)));
    }

    @Override
    @PostMapping("/api/auth/refresh")
    public ResponseEntity<DataResponse<TokenResponse>> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        AuthResult result = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(DataResponse.from(AuthMapper.toTokenResponse(result)));
    }

    @Override
    @PostMapping("/api/auth/logout")
    public ResponseEntity<DataResponse<Void>> logout(@RequestBody @Valid RefreshTokenRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        authService.logout(currentUserId, request.refreshToken());
        return ResponseEntity.ok(DataResponse.from(null));
    }
}
