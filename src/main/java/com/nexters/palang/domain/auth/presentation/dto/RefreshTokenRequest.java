package com.nexters.palang.domain.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.refresh")
        String refreshToken
) {
}
