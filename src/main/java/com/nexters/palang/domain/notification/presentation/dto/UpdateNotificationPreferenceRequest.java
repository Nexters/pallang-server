package com.nexters.palang.domain.notification.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(
        @NotNull(message = "활동 알림 설정 값은 필수입니다.")
        @Schema(description = "활동(좋아요/댓글 등) 알림 수신 여부", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean activityEnabled,
        @NotNull(message = "모임 알림 설정 값은 필수입니다.")
        @Schema(description = "모임(가입/일정 변경/종료 임박 등) 알림 수신 여부", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean groupEnabled
) {
}
