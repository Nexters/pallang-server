package com.nexters.palang.domain.notification.application;

import com.nexters.palang.domain.notification.domain.Notification;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.notification.infrastructure.NotificationRepository;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 알림 생성 공통 경로: 자기알림 제외, 저장, 푸시 트리거를 한 곳에서 처리한다.
@Service
@RequiredArgsConstructor
public class NotificationCreationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationPushDispatcher pushDispatcher;

    @Transactional
    public void create(
            Long receiverId,
            Long actorId,
            NotificationType type,
            String title,
            String body,
            Long opinionId,
            Long bookId,
            Long commentId,
            Integer opinionCountSnapshot
    ) {
        if (actorId != null && actorId.equals(receiverId)) {
            return;
        }

        User receiver = userRepository.getReferenceById(receiverId);
        Notification notification = Notification.builder()
                .receiver(receiver)
                .type(type)
                .title(title)
                .body(body)
                .actorId(actorId)
                .opinionId(opinionId)
                .bookId(bookId)
                .commentId(commentId)
                .opinionCountSnapshot(opinionCountSnapshot)
                .build();
        notificationRepository.save(notification);

        pushDispatcher.dispatch(receiverId, type, title, body, notification.getId());
    }
}
