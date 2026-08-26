package com.nexters.palang.domain.admin.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminLoginResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String accessToken
) {
}
