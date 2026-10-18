package com.nexters.palang.domain.notification.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record HasUnreadResponse(
        @Schema(description = "scope와 무관하게 안 읽은 알림이 하나라도 있는지 여부") boolean hasUnread
) {
}
