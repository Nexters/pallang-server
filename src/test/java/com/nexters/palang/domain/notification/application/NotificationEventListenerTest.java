package com.nexters.palang.domain.notification.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.comment.domain.event.CommentCreatedEvent;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.opinion.domain.event.OpinionCreatedEvent;
import com.nexters.palang.domain.opinion.domain.event.OpinionLikedEvent;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationCreationService notificationCreationService;

    @Mock
    private BookNewOpinionsNotifier bookNewOpinionsNotifier;

    @Mock
    private UserRepository userRepository;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(notificationCreationService, bookNewOpinionsNotifier, userRepository);
    }

    private User user(Long id) {
        User user = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("좋아요 이벤트를 받으면 흔적 작성자에게 OPINION_LIKED 알림 생성을 요청한다")
    void onOpinionLikedCreatesNotification() {
        given(userRepository.findById(2L)).willReturn(Optional.of(user(2L)));

        listener.onOpinionLiked(new OpinionLikedEvent(10L, 1L, 2L));

        verify(notificationCreationService).create(
                eq(1L), eq(2L), eq(NotificationType.OPINION_LIKED), any(), any(), eq(10L), eq(null), eq(null), eq(null));
    }

    @Test
    @DisplayName("좋아요를 누른 사용자를 찾을 수 없으면 알림을 생성하지 않는다")
    void onOpinionLikedSkipsWhenActorMissing() {
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        listener.onOpinionLiked(new OpinionLikedEvent(10L, 1L, 2L));

        verify(notificationCreationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("댓글 생성 이벤트를 받으면 흔적 작성자에게 OPINION_COMMENTED 알림 생성을 요청한다")
    void onCommentCreatedCreatesNotification() {
        given(userRepository.findById(2L)).willReturn(Optional.of(user(2L)));

        listener.onCommentCreated(new CommentCreatedEvent(5L, 10L, 1L, 2L));

        verify(notificationCreationService).create(
                eq(1L), eq(2L), eq(NotificationType.OPINION_COMMENTED), any(), any(), eq(10L), eq(null), eq(5L), eq(null));
    }

    @Test
    @DisplayName("의견 생성 이벤트를 받으면 BookNewOpinionsNotifier에 위임한다")
    void onOpinionCreatedDelegatesToBookNewOpinionsNotifier() {
        listener.onOpinionCreated(new OpinionCreatedEvent(20L, 3L, 2L));

        verify(bookNewOpinionsNotifier).notifyIfThresholdReached(3L, 2L);
    }
}
