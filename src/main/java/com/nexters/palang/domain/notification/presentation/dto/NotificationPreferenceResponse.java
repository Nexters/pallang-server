package com.nexters.palang.domain.notification.presentation.dto;

import com.nexters.palang.domain.notification.domain.NotificationPreference;
import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationPreferenceResponse(
        @Schema(description = "푸시 알림 전체 마스터 스위치") boolean pushEnabled,
        @Schema(description = "활동 - 의견 및 댓글: 내가 의견을 남긴 곳에 의견·댓글이 달릴 때 (전역)") boolean activityCommentEnabled,
        @Schema(description = "활동 - 좋아요: 내 의견에 남겨진 좋아요 (전역)") boolean activityLikeEnabled,
        @Schema(description = "모임 - 초대 및 가입: 모임 초대 및 가입 승인") boolean groupInviteJoinEnabled,
        @Schema(description = "모임 - 의견 및 댓글: 내가 의견을 남긴 모임 대목에 의견·댓글이 달릴 때") boolean groupCommentEnabled,
        @Schema(description = "모임 - 좋아요: 내 모임 의견에 남겨진 좋아요") boolean groupLikeEnabled,
        @Schema(description = "모임 - 일정 변경: 모임 일정이 변경되었을 때, 일정이 끝나갈 때") boolean groupScheduleEnabled
) {

    private static final boolean DEFAULT_ENABLED = true;

    public static NotificationPreferenceResponse defaultSettings() {
        return new NotificationPreferenceResponse(
                DEFAULT_ENABLED, DEFAULT_ENABLED, DEFAULT_ENABLED, DEFAULT_ENABLED,
                DEFAULT_ENABLED, DEFAULT_ENABLED, DEFAULT_ENABLED);
    }

    public static NotificationPreferenceResponse from(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.isPushEnabled(),
                preference.isActivityCommentEnabled(),
                preference.isActivityLikeEnabled(),
                preference.isGroupInviteJoinEnabled(),
                preference.isGroupCommentEnabled(),
                preference.isGroupLikeEnabled(),
                preference.isGroupScheduleEnabled());
    }
}
