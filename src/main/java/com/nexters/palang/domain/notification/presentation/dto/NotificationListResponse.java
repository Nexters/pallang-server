package com.nexters.palang.domain.notification.presentation.dto;

import com.nexters.palang.domain.notification.domain.Notification;
import com.nexters.palang.global.common.response.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

public record NotificationListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<NotificationResponse> notifications,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageInfo pageInfo
) {
    public static NotificationListResponse from(Page<Notification> page) {
        List<NotificationResponse> notifications = page.getContent().stream()
                .map(NotificationResponse::from)
                .toList();
        return new NotificationListResponse(notifications, PageInfo.from(page));
    }
}
