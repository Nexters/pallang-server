package com.nexters.palang.domain.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.access") String accessToken,
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.refresh") String refreshToken,
        @Schema(example = "true") boolean isNewUser,
        @Schema(example = "false") boolean termsAgreed,
        @Schema(example = "false") boolean hasCompletedOnboarding
) {
}
