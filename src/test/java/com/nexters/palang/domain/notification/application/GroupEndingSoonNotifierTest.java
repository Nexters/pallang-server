package com.nexters.palang.domain.notification.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.notification.infrastructure.NotificationRepository;
import com.nexters.palang.domain.user.domain.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GroupEndingSoonNotifierTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationCreationService notificationCreationService;

    private GroupEndingSoonNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new GroupEndingSoonNotifier(
                groupRepository, groupMemberRepository, notificationRepository, notificationCreationService);
    }

    private Group group(Long id) {
        Book book = Book.builder().title("제목").author("작가").publisher("출판사").pageCount(300).build();
        User host = User.builder().nickname("호스트").build();
        ReflectionTestUtils.setField(host, "id", 1L);
        Group built = Group.create(
                "모임", book, host, 4, LocalDate.now(), LocalDate.now().plusDays(1));
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    @Test
    @DisplayName("내일 종료되는 모임의 멤버 전원에게 종료 임박 알림을 생성한다")
    void notifiesAllMembersOfGroupsEndingTomorrow() {
        Group group = group(9L);
        given(groupRepository.findAllByEndDate(LocalDate.now().plusDays(1))).willReturn(List.of(group));
        given(groupMemberRepository.findUserIdsByGroupId(9L)).willReturn(List.of(1L, 2L));
        given(notificationRepository.existsByReceiverIdAndTypeAndGroupIdAndCreatedAtAfter(
                any(), eq(NotificationType.GROUP_ENDING_SOON), eq(9L), any(LocalDateTime.class)))
                .willReturn(false);

        notifier.notifyGroupsEndingTomorrow();

        verify(notificationCreationService).create(eq(1L), eq(null), eq(NotificationType.GROUP_ENDING_SOON), any(),
                any(), eq(null), eq(null), eq(null), eq(9L), eq("모임"), eq(null));
        verify(notificationCreationService).create(eq(2L), eq(null), eq(NotificationType.GROUP_ENDING_SOON), any(),
                any(), eq(null), eq(null), eq(null), eq(9L), eq("모임"), eq(null));
    }

    @Test
    @DisplayName("오늘 이미 종료 임박 알림을 받은 멤버에게는 다시 생성하지 않는다")
    void doesNotNotifyTwiceOnSameDay() {
        Group group = group(9L);
        given(groupRepository.findAllByEndDate(LocalDate.now().plusDays(1))).willReturn(List.of(group));
        given(groupMemberRepository.findUserIdsByGroupId(9L)).willReturn(List.of(1L));
        given(notificationRepository.existsByReceiverIdAndTypeAndGroupIdAndCreatedAtAfter(
                eq(1L), eq(NotificationType.GROUP_ENDING_SOON), eq(9L), any(LocalDateTime.class)))
                .willReturn(true);

        notifier.notifyGroupsEndingTomorrow();

        verify(notificationCreationService, never()).create(
                eq(1L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("내일 종료되는 모임이 없으면 아무 알림도 생성하지 않는다")
    void doesNothingWhenNoGroupsEndingTomorrow() {
        given(groupRepository.findAllByEndDate(LocalDate.now().plusDays(1))).willReturn(List.of());

        notifier.notifyGroupsEndingTomorrow();

        verify(notificationCreationService, never()).create(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
