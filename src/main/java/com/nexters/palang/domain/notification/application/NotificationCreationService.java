package com.nexters.palang.domain.notification.application;

import com.nexters.palang.domain.notification.domain.Notification;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.notification.domain.RemovalReason;
import com.nexters.palang.domain.notification.infrastructure.NotificationRepository;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 알림 생성 공통 경로: 자기알림 제외, 저장, 푸시 트리거를 한 곳에서 처리한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCreationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationPushDispatcher pushDispatcher;
    private final NotificationPreferenceService notificationPreferenceService;

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
            Long groupId,
            String groupName,
            Integer opinionCountSnapshot
    ) {
        if (actorId != null && actorId.equals(receiverId)) {
            return;
        }
        if (!notificationPreferenceService.isEnabled(receiverId, type, groupId)) {
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
                .groupId(groupId)
                .groupName(groupName)
                .opinionCountSnapshot(opinionCountSnapshot)
                .build();
        notificationRepository.save(notification);

        try {
            pushDispatcher.dispatch(receiverId, type, title, body, notification.getId());
        } catch (Exception e) {
            // 푸시 발송 실패가 방금 저장한 알림까지 롤백시키면 안 된다 (인앱 알림은 이미 유효한 결과).
            log.error("FCM 푸시 발송 실패 (notificationId={})", notification.getId(), e);
        }
    }

    // 운영 안내(관리자 사유 선택 삭제). 작성자가 반드시 알아야 하는 내용이라 알림 설정과 무관하게 인앱에는
    // 항상 저장하고, 푸시만 사용자의 푸시 마스터 설정을 따른다.
    @Transactional
    public void createContentRemovedNotice(
            Long receiverId, RemovalReason reason, String bookTitle, LocalDateTime writtenAt, String content) {
        User receiver = userRepository.getReferenceById(receiverId);
        Notification notification = notificationRepository.save(
                Notification.contentRemoved(receiver, reason, bookTitle, writtenAt, content));
        if (!notificationPreferenceService.isPushEnabled(receiverId)) {
            return;
        }
        try {
            pushDispatcher.dispatch(receiverId, NotificationType.CONTENT_REMOVED,
                    notification.getTitle(), notification.getBody(), notification.getId());
        } catch (Exception e) {
            log.error("FCM 푸시 발송 실패 (notificationId={})", notification.getId(), e);
        }
    }

    // OPINION_LIKED 전용 집계 경로(Figma 기준 "N명이 좋아해요"): 같은 흔적에 대해 아직 안 읽은 좋아요 알림이
    // 있으면 새로 만들지 않고 그 알림의 제목(인원수)/행위자만 갱신한다. 없거나 이미 읽었으면 새로 만든다.
    @Transactional
    public void createOrUpdateOpinionLiked(
            Long receiverId, Long actorId, String title, String body,
            Long opinionId, Long groupId, String groupName
    ) {
        if (actorId != null && actorId.equals(receiverId)) {
            return;
        }
        if (!notificationPreferenceService.isEnabled(receiverId, NotificationType.OPINION_LIKED, groupId)) {
            return;
        }

        Notification notification = notificationRepository
                .findFirstByReceiverIdAndTypeAndOpinionIdAndReadFalseOrderByCreatedAtDesc(
                        receiverId, NotificationType.OPINION_LIKED, opinionId)
                .map(existing -> {
                    existing.refreshLikeAggregation(actorId, title, body);
                    return existing;
                })
                .orElseGet(() -> {
                    User receiver = userRepository.getReferenceById(receiverId);
                    Notification created = Notification.builder()
                            .receiver(receiver)
                            .type(NotificationType.OPINION_LIKED)
                            .title(title)
                            .body(body)
                            .actorId(actorId)
                            .opinionId(opinionId)
                            .groupId(groupId)
                            .groupName(groupName)
                            .build();
                    return notificationRepository.save(created);
                });

        try {
            pushDispatcher.dispatch(receiverId, NotificationType.OPINION_LIKED, title, body, notification.getId());
        } catch (Exception e) {
            log.error("FCM 푸시 발송 실패 (notificationId={})", notification.getId(), e);
        }
    }
}
