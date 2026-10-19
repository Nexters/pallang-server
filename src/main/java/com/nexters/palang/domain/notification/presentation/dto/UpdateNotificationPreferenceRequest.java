package com.nexters.palang.domain.notification.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(
        @NotNull(message = "푸시 알림 설정 값은 필수입니다.")
        @Schema(description = "푸시 알림 전체 마스터 스위치", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean pushEnabled,
        @NotNull(message = "활동 - 의견 및 댓글 설정 값은 필수입니다.")
        @Schema(description = "활동 - 의견 및 댓글: 내가 의견을 남긴 곳에 의견·댓글이 달릴 때 (전역)", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean activityCommentEnabled,
        @NotNull(message = "활동 - 좋아요 설정 값은 필수입니다.")
        @Schema(description = "활동 - 좋아요: 내 의견에 남겨진 좋아요 (전역)", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean activityLikeEnabled,
        @NotNull(message = "모임 - 초대 및 가입 설정 값은 필수입니다.")
        @Schema(description = "모임 - 초대 및 가입: 모임 초대 및 가입 승인", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean groupInviteJoinEnabled,
        @NotNull(message = "모임 - 의견 및 댓글 설정 값은 필수입니다.")
        @Schema(description = "모임 - 의견 및 댓글: 내가 의견을 남긴 모임 대목에 의견·댓글이 달릴 때", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean groupCommentEnabled,
        @NotNull(message = "모임 - 좋아요 설정 값은 필수입니다.")
        @Schema(description = "모임 - 좋아요: 내 모임 의견에 남겨진 좋아요", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean groupLikeEnabled,
        @NotNull(message = "모임 - 일정 변경 설정 값은 필수입니다.")
        @Schema(description = "모임 - 일정 변경: 모임 일정이 변경되었을 때, 일정이 끝나갈 때", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean groupScheduleEnabled
) {
}
