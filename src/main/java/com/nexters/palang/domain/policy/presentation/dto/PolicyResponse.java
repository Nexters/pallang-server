package com.nexters.palang.domain.policy.presentation.dto;

import com.nexters.palang.domain.policy.domain.PolicyType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record PolicyResponse(
        @Schema(example = "TERMS", requiredMode = Schema.RequiredMode.REQUIRED) PolicyType policyType,
        @Schema(example = "# 이용약관\n\n## 1. 서비스 목적\n...", requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(example = "2026-07-20T10:15:30", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime updatedAt
) {
}
