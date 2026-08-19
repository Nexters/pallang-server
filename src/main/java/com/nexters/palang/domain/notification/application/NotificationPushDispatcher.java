package com.nexters.palang.domain.notification.application;

import com.nexters.palang.domain.notification.domain.DeviceToken;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.notification.infrastructure.DeviceTokenRepository;
import com.nexters.palang.domain.notification.infrastructure.fcm.FcmPushSender;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationPushDispatcher {

    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmPushSender fcmPushSender;

    @Transactional
    public void dispatch(Long receiverId, NotificationType type, String title, String body, Long notificationId) {
        List<DeviceToken> deviceTokens = deviceTokenRepository.findAllByUserId(receiverId);
        if (deviceTokens.isEmpty()) {
            return;
        }

        List<String> tokens = deviceTokens.stream().map(DeviceToken::getToken).toList();
        Map<String, String> data = Map.of(
                "type", type.name(),
                "notificationId", String.valueOf(notificationId)
        );

        List<String> invalidTokens = fcmPushSender.sendMulticast(tokens, title, body, data);
        invalidTokens.forEach(token -> deviceTokenRepository.findByToken(token)
                .ifPresent(deviceTokenRepository::delete));
    }
}
