package com.nexters.palang.domain.notification.presentation.dto;

import com.nexters.palang.domain.notification.domain.NotificationPreference;
import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationPreferenceResponse(
        @Schema(description = "활동(좋아요/댓글 등) 알림 수신 여부") boolean activityEnabled,
        @Schema(description = "모임(가입/일정 변경/종료 임박 등) 알림 수신 여부") boolean groupEnabled
) {

    private static final boolean DEFAULT_ENABLED = true;

    public static NotificationPreferenceResponse defaultSettings() {
        return new NotificationPreferenceResponse(DEFAULT_ENABLED, DEFAULT_ENABLED);
    }

    public static NotificationPreferenceResponse from(NotificationPreference preference) {
        return new NotificationPreferenceResponse(preference.isActivityEnabled(), preference.isGroupEnabled());
    }
}
