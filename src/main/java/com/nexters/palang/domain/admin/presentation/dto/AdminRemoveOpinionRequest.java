package com.nexters.palang.domain.admin.presentation.dto;

import com.nexters.palang.domain.notification.domain.RemovalReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AdminRemoveOpinionRequest(
        @NotNull(message = "삭제 사유를 선택해주세요.")
        @Schema(description = "삭제 사유 — 사유에 맞는 안내 문구가 작성자에게 발송됩니다", requiredMode = Schema.RequiredMode.REQUIRED)
        RemovalReason reason
) {
}
