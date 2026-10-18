package com.nexters.palang.domain.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.notification.domain.Notification;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class NotificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    private User receiver;

    @BeforeEach
    void setUp() {
        receiver = entityManager.persistAndFlush(User.builder()
                .nickname("수신자")
                .snsProvider(SnsProvider.KAKAO)
                .snsId("receiver")
                .termsAgreedAt(LocalDateTime.now())
                .build());
    }

    private Notification save(NotificationType type, Long bookId, Integer snapshot) {
        return save(type, bookId, snapshot, null, null);
    }

    private Notification save(NotificationType type, Long bookId, Integer snapshot, Long groupId, String groupName) {
        Notification notification = Notification.builder()
                .receiver(receiver)
                .type(type)
                .title("제목").body("내용")
                .bookId(bookId)
                .groupId(groupId)
                .groupName(groupName)
                .opinionCountSnapshot(snapshot)
                .build();
        return entityManager.persistAndFlush(notification);
    }

    @Test
    @DisplayName("알림 목록을 조회하면 최신순으로 반환한다")
    void findByReceiverIdOrderByCreatedAtDesc() {
        Notification older = save(NotificationType.OPINION_LIKED, null, null);
        Notification newer = save(NotificationType.OPINION_COMMENTED, null, null);

        Page<Notification> results = notificationRepository
                .findByReceiverIdOrderByCreatedAtDesc(receiver.getId(), PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(Notification::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    @DisplayName("ACTIVITY 스코프로 조회하면 모임에 속하지 않은 알림만 최신순으로 반환한다")
    void findByReceiverIdAndGroupIdIsNullOrderByCreatedAtDesc() {
        Notification activity = save(NotificationType.OPINION_LIKED, null, null, null, null);
        save(NotificationType.OPINION_LIKED, null, null, 9L, "모임");

        Page<Notification> results = notificationRepository
                .findByReceiverIdAndGroupIdIsNullOrderByCreatedAtDesc(receiver.getId(), PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(Notification::getId).containsExactly(activity.getId());
    }

    @Test
    @DisplayName("GROUP 스코프로 조회하면 모임에 속한 알림만 최신순으로 반환한다")
    void findByReceiverIdAndGroupIdIsNotNullOrderByCreatedAtDesc() {
        save(NotificationType.OPINION_LIKED, null, null, null, null);
        Notification groupNotification = save(NotificationType.OPINION_LIKED, null, null, 9L, "모임");

        Page<Notification> results = notificationRepository
                .findByReceiverIdAndGroupIdIsNotNullOrderByCreatedAtDesc(receiver.getId(), PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(Notification::getId).containsExactly(groupNotification.getId());
    }

    @Test
    @DisplayName("오늘 보낸 GROUP_ENDING_SOON 알림이 있으면 true를 반환한다")
    void existsByReceiverIdAndTypeAndGroupIdAndCreatedAtAfterReturnsTrueWhenNotifiedToday() {
        save(NotificationType.GROUP_ENDING_SOON, null, null, 9L, "모임");

        boolean exists = notificationRepository.existsByReceiverIdAndTypeAndGroupIdAndCreatedAtAfter(
                receiver.getId(), NotificationType.GROUP_ENDING_SOON, 9L, LocalDateTime.now().toLocalDate().atStartOfDay());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("다른 모임에 보낸 알림은 포함하지 않는다")
    void existsByReceiverIdAndTypeAndGroupIdAndCreatedAtAfterExcludesOtherGroups() {
        save(NotificationType.GROUP_ENDING_SOON, null, null, 10L, "다른 모임");

        boolean exists = notificationRepository.existsByReceiverIdAndTypeAndGroupIdAndCreatedAtAfter(
                receiver.getId(), NotificationType.GROUP_ENDING_SOON, 9L, LocalDateTime.now().toLocalDate().atStartOfDay());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("안 읽은 전체 개수를 집계한다")
    void countByReceiverIdAndReadFalse() {
        save(NotificationType.OPINION_LIKED, null, null, null, null);
        Notification groupNotification = save(NotificationType.OPINION_LIKED, null, null, 9L, "모임");
        groupNotification.markAsRead();
        entityManager.persistAndFlush(groupNotification);

        long count = notificationRepository.countByReceiverIdAndReadFalse(receiver.getId());

        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("활동 탭의 안 읽은 개수만 집계한다")
    void countByReceiverIdAndGroupIdIsNullAndReadFalse() {
        save(NotificationType.OPINION_LIKED, null, null, null, null);
        save(NotificationType.OPINION_LIKED, null, null, 9L, "모임");

        long count = notificationRepository.countByReceiverIdAndGroupIdIsNullAndReadFalse(receiver.getId());

        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("모임 탭의 안 읽은 개수만 집계한다")
    void countByReceiverIdAndGroupIdIsNotNullAndReadFalse() {
        save(NotificationType.OPINION_LIKED, null, null, null, null);
        save(NotificationType.OPINION_LIKED, null, null, 9L, "모임");

        long count = notificationRepository.countByReceiverIdAndGroupIdIsNotNullAndReadFalse(receiver.getId());

        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("안 읽은 알림이 하나라도 있으면 true를 반환한다")
    void existsByReceiverIdAndReadFalseReturnsTrue() {
        save(NotificationType.OPINION_LIKED, null, null, null, null);

        assertThat(notificationRepository.existsByReceiverIdAndReadFalse(receiver.getId())).isTrue();
    }

    @Test
    @DisplayName("안 읽은 알림이 없으면 false를 반환한다")
    void existsByReceiverIdAndReadFalseReturnsFalse() {
        Notification notification = save(NotificationType.OPINION_LIKED, null, null, null, null);
        notification.markAsRead();
        entityManager.persistAndFlush(notification);

        assertThat(notificationRepository.existsByReceiverIdAndReadFalse(receiver.getId())).isFalse();
    }

    @Test
    @DisplayName("전체 읽음 처리를 하면 안 읽은 알림만 읽음으로 바뀐다")
    void markAllAsRead() {
        Notification unread1 = save(NotificationType.OPINION_LIKED, null, null);
        Notification unread2 = save(NotificationType.OPINION_COMMENTED, null, null);

        int updated = notificationRepository.markAllAsRead(receiver.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(updated).isEqualTo(2);
        assertThat(notificationRepository.findById(unread1.getId()).orElseThrow().isRead()).isTrue();
        assertThat(notificationRepository.findById(unread2.getId()).orElseThrow().isRead()).isTrue();
    }
}
