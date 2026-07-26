package com.nexters.palang.domain.auth.application;

import com.nexters.palang.domain.auth.presentation.dto.LoginResponse;
import com.nexters.palang.domain.auth.presentation.dto.TokenResponse;

public final class AuthMapper {

    private AuthMapper() {
    }

    public static LoginResponse toLoginResponse(AuthResult result) {
        return new LoginResponse(
                result.accessToken(), result.refreshToken(),
                result.isNewUser(), result.termsAgreed(), result.hasCompletedOnboarding());
    }

    public static TokenResponse toTokenResponse(AuthResult result) {
        return new TokenResponse(result.accessToken(), result.refreshToken());
    }
}
