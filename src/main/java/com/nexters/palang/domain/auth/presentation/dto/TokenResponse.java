package com.nexters.palang.domain.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.access") String accessToken,
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.refresh") String refreshToken
) {
}
