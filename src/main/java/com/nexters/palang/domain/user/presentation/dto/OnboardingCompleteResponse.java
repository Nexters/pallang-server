package com.nexters.palang.domain.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record OnboardingCompleteResponse(
        @Schema(example = "true", requiredMode = Schema.RequiredMode.REQUIRED) boolean hasCompletedOnboarding
) {
}
