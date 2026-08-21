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
        @Schema(example = "false", requiredMode = Schema.RequiredMode.REQUIRED) boolean isRead,
        @Schema(example = "2026-08-19T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
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
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
