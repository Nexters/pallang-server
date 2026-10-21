package com.nexters.palang.domain.notification.presentation.dto;

import com.nexters.palang.domain.notification.domain.Notification;
import com.nexters.palang.domain.notification.domain.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record NotificationResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long notificationId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) NotificationType type,
        @Schema(example = "새로운 좋아요", requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(example = "책읽는고양이님이 회원님의 흔적을 좋아합니다.", requiredMode = Schema.RequiredMode.REQUIRED) String body,
        @Schema(example = "3", nullable = true) Long opinionId,
        @Schema(example = "10", nullable = true) Long bookId,
        @Schema(example = "5", nullable = true) Long commentId,
        @Schema(description = "모임 전용 알림이면 그 모임 ID, 전역 알림이면 null", example = "13", nullable = true) Long groupId,
        @Schema(description = "모임 전용 알림이면 그 모임 이름, 전역 알림이면 null", example = "면도날 같이 읽기", nullable = true) String groupName,
        @Schema(example = "false", requiredMode = Schema.RequiredMode.REQUIRED) boolean isRead,
        @Schema(example = "2026-08-19T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt,
        @Schema(description = "type이 CONTENT_REMOVED(운영 안내)일 때만 채워지는 삭제 안내 정보, 그 외에는 null",
                nullable = true) RemovalNoticeResponse removalNotice
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getOpinionId(),
                notification.getBookId(),
                notification.getCommentId(),
                notification.getGroupId(),
                notification.getGroupName(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getType() == NotificationType.CONTENT_REMOVED
                        ? RemovalNoticeResponse.from(notification) : null
        );
    }
}
