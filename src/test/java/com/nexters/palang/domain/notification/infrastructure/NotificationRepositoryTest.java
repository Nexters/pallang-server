package com.nexters.palang.domain.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.notification.domain.Notification;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import java.util.Optional;
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
        Notification notification = Notification.builder()
                .receiver(receiver)
                .type(type)
                .title("제목").body("내용")
                .bookId(bookId)
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
    @DisplayName("같은 책에 대해 가장 최근 BOOK_NEW_OPINIONS 알림의 snapshot을 조회한다")
    void findFirstByReceiverIdAndTypeAndBookIdOrderByCreatedAtDesc() {
        save(NotificationType.BOOK_NEW_OPINIONS, 1L, 5);
        Notification latest = save(NotificationType.BOOK_NEW_OPINIONS, 1L, 10);
        save(NotificationType.BOOK_NEW_OPINIONS, 2L, 99);

        Optional<Notification> found = notificationRepository
                .findFirstByReceiverIdAndTypeAndBookIdOrderByCreatedAtDesc(
                        receiver.getId(), NotificationType.BOOK_NEW_OPINIONS, 1L);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(latest.getId());
        assertThat(found.get().getOpinionCountSnapshot()).isEqualTo(10);
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
