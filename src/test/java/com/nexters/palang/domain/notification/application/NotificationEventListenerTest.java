package com.nexters.palang.domain.notification.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.comment.domain.event.CommentCreatedEvent;
import com.nexters.palang.domain.group.domain.event.GroupJoinedEvent;
import com.nexters.palang.domain.group.domain.event.GroupScheduleChangedEvent;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.opinion.domain.event.OpinionLikedEvent;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.List;
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
    private UserRepository userRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(notificationCreationService, userRepository, groupMemberRepository);
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

        listener.onOpinionLiked(new OpinionLikedEvent(10L, 1L, 2L, null, null));

        verify(notificationCreationService).create(eq(1L), eq(2L), eq(NotificationType.OPINION_LIKED), any(), any(),
                eq(10L), eq(null), eq(null), eq(null), eq(null), eq(null));
    }

    @Test
    @DisplayName("모임 전용 흔적에 대한 좋아요 이벤트면 모임 정보를 그대로 알림 생성에 전달한다")
    void onOpinionLikedPropagatesGroupInfo() {
        given(userRepository.findById(2L)).willReturn(Optional.of(user(2L)));

        listener.onOpinionLiked(new OpinionLikedEvent(10L, 1L, 2L, 9L, "모임 이름"));

        verify(notificationCreationService).create(eq(1L), eq(2L), eq(NotificationType.OPINION_LIKED), any(), any(),
                eq(10L), eq(null), eq(null), eq(9L), eq("모임 이름"), eq(null));
    }

    @Test
    @DisplayName("좋아요를 누른 사용자를 찾을 수 없으면 알림을 생성하지 않는다")
    void onOpinionLikedSkipsWhenActorMissing() {
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        listener.onOpinionLiked(new OpinionLikedEvent(10L, 1L, 2L, null, null));

        verify(notificationCreationService, never()).create(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("댓글 생성 이벤트를 받으면 흔적 작성자에게 OPINION_COMMENTED 알림 생성을 요청한다")
    void onCommentCreatedCreatesNotification() {
        given(userRepository.findById(2L)).willReturn(Optional.of(user(2L)));

        listener.onCommentCreated(new CommentCreatedEvent(5L, 10L, 1L, 2L, null, null));

        verify(notificationCreationService).create(eq(1L), eq(2L), eq(NotificationType.OPINION_COMMENTED), any(),
                any(), eq(10L), eq(null), eq(5L), eq(null), eq(null), eq(null));
    }

    @Test
    @DisplayName("모임 가입 이벤트를 받으면 가입자 본인과 호스트 각각에게 알림 생성을 요청한다")
    void onGroupJoinedCreatesNotificationsForJoinerAndHost() {
        given(userRepository.findById(2L)).willReturn(Optional.of(user(2L)));

        listener.onGroupJoined(new GroupJoinedEvent(9L, "모임 이름", 2L, 1L));

        verify(notificationCreationService).create(eq(2L), eq(null), eq(NotificationType.GROUP_JOINED), any(), any(),
                eq(null), eq(null), eq(null), eq(9L), eq("모임 이름"), eq(null));
        verify(notificationCreationService).create(eq(1L), eq(2L), eq(NotificationType.GROUP_MEMBER_JOINED), any(),
                any(), eq(null), eq(null), eq(null), eq(9L), eq("모임 이름"), eq(null));
    }

    @Test
    @DisplayName("가입자를 찾을 수 없으면 호스트 알림은 생성하지 않지만 가입자 본인 알림은 생성한다")
    void onGroupJoinedSkipsHostNotificationWhenJoinerMissing() {
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        listener.onGroupJoined(new GroupJoinedEvent(9L, "모임 이름", 2L, 1L));

        verify(notificationCreationService).create(eq(2L), eq(null), eq(NotificationType.GROUP_JOINED), any(), any(),
                eq(null), eq(null), eq(null), eq(9L), eq("모임 이름"), eq(null));
        verify(notificationCreationService, never()).create(
                eq(1L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("일정 변경 이벤트를 받으면 변경한 호스트를 제외한 멤버 전원에게 알림 생성을 요청한다")
    void onGroupScheduleChangedNotifiesMembersExceptActor() {
        given(groupMemberRepository.findUserIdsByGroupId(9L)).willReturn(List.of(1L, 2L, 3L));

        listener.onGroupScheduleChanged(new GroupScheduleChangedEvent(9L, "모임 이름", 1L));

        verify(notificationCreationService).create(eq(2L), eq(1L), eq(NotificationType.GROUP_SCHEDULE_CHANGED), any(),
                any(), eq(null), eq(null), eq(null), eq(9L), eq("모임 이름"), eq(null));
        verify(notificationCreationService).create(eq(3L), eq(1L), eq(NotificationType.GROUP_SCHEDULE_CHANGED), any(),
                any(), eq(null), eq(null), eq(null), eq(9L), eq("모임 이름"), eq(null));
        verify(notificationCreationService, never()).create(
                eq(1L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
