package com.nexters.palang.domain.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.notification.common.error.NotificationException;
import com.nexters.palang.domain.notification.domain.Notification;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.notification.infrastructure.NotificationRepository;
import com.nexters.palang.domain.user.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    private User user(Long id) {
        User user = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Notification notification(Long id, User receiver) {
        Notification notification = Notification.builder()
                .receiver(receiver)
                .type(NotificationType.OPINION_LIKED)
                .title("제목").body("내용")
                .build();
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    @Test
    @DisplayName("알림 목록을 조회하면 Repository 결과를 그대로 반환한다")
    void getNotificationsReturnsPageFromRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Notification> expected = new PageImpl<>(java.util.List.of(notification(1L, user(1L))), pageable, 1);
        given(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable)).willReturn(expected);

        Page<Notification> results = notificationService.getNotifications(1L, pageable);

        assertThat(results).isEqualTo(expected);
    }

    @Test
    @DisplayName("본인이 받은 알림을 읽음 처리하면 읽음 상태가 된다")
    void markAsReadUpdatesNotification() {
        Notification notification = notification(1L, user(10L));
        given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

        notificationService.markAsRead(10L, 1L);

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    @DisplayName("본인이 받은 알림이 아니면 읽음 처리 시 예외가 발생한다")
    void markAsReadThrowsExceptionWhenNotOwner() {
        Notification notification = notification(1L, user(10L));
        given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(999L, 1L))
                .isInstanceOf(NotificationException.class);
    }

    @Test
    @DisplayName("존재하지 않는 알림을 읽음 처리하면 예외가 발생한다")
    void markAsReadThrowsExceptionWhenNotFound() {
        given(notificationRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(10L, 999L))
                .isInstanceOf(NotificationException.class);
    }

    @Test
    @DisplayName("전체 읽음 처리를 요청하면 Repository의 bulk update를 호출한다")
    void markAllAsReadDelegatesToRepository() {
        notificationService.markAllAsRead(10L);

        verify(notificationRepository).markAllAsRead(10L);
    }
}
