package com.nexters.palang.domain.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @NotBlank(message = "카카오 액세스 토큰은 필수입니다.")
        @Schema(example = "kakao-access-token")
        String kakaoAccessToken
) {
}
