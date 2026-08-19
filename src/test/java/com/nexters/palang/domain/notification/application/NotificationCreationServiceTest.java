package com.nexters.palang.domain.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.notification.domain.Notification;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.notification.infrastructure.NotificationRepository;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationCreationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationPushDispatcher pushDispatcher;

    private NotificationCreationService notificationCreationService;

    @BeforeEach
    void setUp() {
        notificationCreationService = new NotificationCreationService(notificationRepository, userRepository, pushDispatcher);
    }

    private User user(Long id) {
        User user = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("본인이 유발한 알림(actorId == receiverId)이면 생성/발송하지 않는다")
    void doesNotCreateSelfNotification() {
        notificationCreationService.create(
                1L, 1L, NotificationType.OPINION_LIKED, "제목", "내용", 10L, null, null, null);

        verify(notificationRepository, never()).save(any());
        verify(pushDispatcher, never()).dispatch(anyLong(), any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("타인이 유발한 알림이면 저장하고 푸시를 발송한다")
    void createsNotificationAndDispatchesPush() {
        given(userRepository.getReferenceById(1L)).willReturn(user(1L));
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 100L);
            return notification;
        });

        notificationCreationService.create(
                1L, 2L, NotificationType.OPINION_LIKED, "제목", "내용", 10L, null, null, null);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("제목");
        assertThat(captor.getValue().getActorId()).isEqualTo(2L);
        verify(pushDispatcher).dispatch(1L, NotificationType.OPINION_LIKED, "제목", "내용", 100L);
    }

    @Test
    @DisplayName("actorId가 없는 알림(BOOK_NEW_OPINIONS)도 정상적으로 생성된다")
    void createsNotificationWithoutActor() {
        given(userRepository.getReferenceById(3L)).willReturn(user(3L));
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 200L);
            return notification;
        });

        notificationCreationService.create(
                3L, null, NotificationType.BOOK_NEW_OPINIONS, "새 의견", "내용", null, 5L, null, 7);

        verify(notificationRepository).save(any(Notification.class));
        verify(pushDispatcher).dispatch(3L, NotificationType.BOOK_NEW_OPINIONS, "새 의견", "내용", 200L);
    }

    @Test
    @DisplayName("푸시 발송이 실패해도 예외를 전파하지 않는다 (방금 저장한 알림이 롤백되면 안 된다)")
    void doesNotPropagateExceptionWhenPushDispatchFails() {
        given(userRepository.getReferenceById(1L)).willReturn(user(1L));
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 100L);
            return notification;
        });
        willThrow(new RuntimeException("FCM 초기화 실패"))
                .given(pushDispatcher).dispatch(anyLong(), any(), any(), any(), anyLong());

        assertThatCode(() -> notificationCreationService.create(
                1L, 2L, NotificationType.OPINION_LIKED, "제목", "내용", 10L, null, null, null))
                .doesNotThrowAnyException();

        verify(notificationRepository).save(any(Notification.class));
    }
}
