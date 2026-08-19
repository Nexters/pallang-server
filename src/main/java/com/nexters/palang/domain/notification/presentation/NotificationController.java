package com.nexters.palang.domain.notification.presentation;

import com.nexters.palang.domain.notification.application.NotificationService;
import com.nexters.palang.domain.notification.presentation.dto.NotificationListResponse;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @GetMapping("/api/notifications")
    public ResponseEntity<DataResponse<NotificationListResponse>> getNotifications(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(NotificationListResponse.from(
                notificationService.getNotifications(currentUserId, pageable(page, size)))));
    }

    @Override
    @PatchMapping("/api/notifications/{notificationId}/read")
    public ResponseEntity<DataResponse<Void>> readNotification(@PathVariable Long notificationId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        notificationService.markAsRead(currentUserId, notificationId);
        return ResponseEntity.ok(DataResponse.from(null));
    }

    @Override
    @PatchMapping("/api/notifications/read-all")
    public ResponseEntity<DataResponse<Void>> readAllNotifications() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok(DataResponse.from(null));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
