package com.nexters.palang.domain.notification.application;

import com.nexters.palang.domain.notification.common.error.NotificationErrorCode;
import com.nexters.palang.domain.notification.common.error.NotificationException;
import com.nexters.palang.domain.notification.domain.Notification;
import com.nexters.palang.domain.notification.domain.NotificationScope;
import com.nexters.palang.domain.notification.infrastructure.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Page<Notification> getNotifications(Long userId, NotificationScope scope, Pageable pageable) {
        return switch (scope) {
            case ALL -> notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId, pageable);
            case ACTIVITY -> notificationRepository.findByReceiverIdAndGroupIdIsNullOrderByCreatedAtDesc(userId, pageable);
            case GROUP -> notificationRepository.findByReceiverIdAndGroupIdIsNotNullOrderByCreatedAtDesc(userId, pageable);
        };
    }

    // 알림함 탭([전체|활동|모임])에 표시할 안 읽은 개수. 조회한 scope와 동일한 범위로 집계한다.
    public long countUnread(Long userId, NotificationScope scope) {
        return switch (scope) {
            case ALL -> notificationRepository.countByReceiverIdAndReadFalse(userId);
            case ACTIVITY -> notificationRepository.countByReceiverIdAndGroupIdIsNullAndReadFalse(userId);
            case GROUP -> notificationRepository.countByReceiverIdAndGroupIdIsNotNullAndReadFalse(userId);
        };
    }

    // 홈 화면 벨 아이콘: scope와 무관하게 안 읽은 알림이 하나라도 있는지만 확인.
    public boolean hasUnread(Long userId) {
        return notificationRepository.existsByReceiverIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getReceiver().getId().equals(userId)) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }
}
