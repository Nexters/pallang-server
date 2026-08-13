package com.nexters.palang.domain.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TermsAgreementResponse(
        @Schema(example = "true", requiredMode = Schema.RequiredMode.REQUIRED) boolean termsAgreed
) {
}
