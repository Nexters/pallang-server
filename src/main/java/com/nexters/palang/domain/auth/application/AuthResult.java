package com.nexters.palang.domain.auth.application;

public record AuthResult(
        String accessToken,
        String refreshToken,
        boolean isNewUser,
        boolean termsAgreed,
        boolean hasCompletedOnboarding
) {
}
