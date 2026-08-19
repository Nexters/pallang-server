package com.nexters.palang.domain.notification.application;

import com.nexters.palang.domain.notification.domain.DeviceToken;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.notification.infrastructure.DeviceTokenRepository;
import com.nexters.palang.domain.notification.infrastructure.fcm.FcmPushSender;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationPushDispatcher {

    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmPushSender fcmPushSender;

    // REQUIRES_NEW: 알림 저장 트랜잭션과 분리해서, 여기서 나는 예외가 방금 저장한 Notification까지
    // 롤백시키지 않게 한다 (호출부인 NotificationCreationService.create()의 try-catch와 한 쌍).
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
