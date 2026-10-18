package com.nexters.palang.domain.notification.presentation;

import com.nexters.palang.domain.notification.application.NotificationPreferenceService;
import com.nexters.palang.domain.notification.presentation.dto.NotificationPreferenceResponse;
import com.nexters.palang.domain.notification.presentation.dto.UpdateNotificationPreferenceRequest;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationPreferenceController implements NotificationPreferenceApi {

    private final NotificationPreferenceService notificationPreferenceService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @GetMapping("/api/notifications/settings")
    public ResponseEntity<DataResponse<NotificationPreferenceResponse>> getSettings() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(notificationPreferenceService.getSettings(currentUserId)));
    }

    @Override
    @PutMapping("/api/notifications/settings")
    public ResponseEntity<DataResponse<Void>> updateSettings(@RequestBody @Valid UpdateNotificationPreferenceRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        notificationPreferenceService.updateSettings(currentUserId, request.activityEnabled(), request.groupEnabled());
        return ResponseEntity.ok(DataResponse.from(null));
    }
}
