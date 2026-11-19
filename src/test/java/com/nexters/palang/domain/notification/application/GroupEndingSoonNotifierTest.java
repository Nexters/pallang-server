package com.nexters.palang.domain.notification.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.ReadingPeriod;
import com.nexters.palang.domain.group.domain.ReadingStatus;
import com.nexters.palang.domain.group.infrastructure.GroupBookEntryRepository;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.notification.infrastructure.NotificationRepository;
import com.nexters.palang.domain.user.domain.User;
import java.time.LocalDate;
import java.time.ZoneId;
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

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private GroupBookEntryRepository groupBookEntryRepository;

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
                groupBookEntryRepository, groupMemberRepository, notificationRepository, notificationCreationService);
    }

    private Group group(Long id) {
        User host = User.builder().nickname("호스트").build();
        ReflectionTestUtils.setField(host, "id", 1L);
        Group built = Group.create("모임", host, 4);
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    // 내일 끝나는 모임 책
    private GroupBookEntry entryEndingTomorrow(Group group) {
        return entryEndingTomorrow(group, "제목");
    }

    private GroupBookEntry entryEndingTomorrow(Group group, String title) {
        Book book = Book.builder().title(title).author("작가").publisher("출판사").pageCount(300).build();
        LocalDate today = LocalDate.now(ZONE);
        return GroupBookEntry.create(group, book, ReadingPeriod.of(today, today.plusDays(1)), today);
    }

    @Test
    @DisplayName("알림 문구에는 끝나는 책 제목이 들어가고, 여러 권이면 첫 책 제목과 나머지 권수로 묶는다")
    void bodyNamesEndingBooks() {
        Group single = group(9L);
        Group multiple = group(10L);
        given(groupBookEntryRepository.findAllInActiveGroupsByEndDate(LocalDate.now(ZONE).plusDays(1)))
                .willReturn(List.of(entryEndingTomorrow(single, "채식주의자"),
                        entryEndingTomorrow(multiple, "모순"), entryEndingTomorrow(multiple, "흰")));
        given(groupMemberRepository.findActiveUserIdsByGroupId(9L)).willReturn(List.of(1L));
        given(groupMemberRepository.findActiveUserIdsByGroupId(10L)).willReturn(List.of(2L));
        given(notificationRepository.existsByReceiverIdAndTypeAndGroupIdAndCreatedAtAfter(
                any(), eq(NotificationType.GROUP_ENDING_SOON), any(), any(LocalDateTime.class)))
                .willReturn(false);

        notifier.notifyGroupsEndingTomorrow();

        verify(notificationCreationService).create(eq(1L), eq(null), eq(NotificationType.GROUP_ENDING_SOON), any(),
                eq("'모임' 모임의 '채식주의자' 읽기가 내일 끝나요."), eq(null), eq(null), eq(null), eq(9L), eq("모임"), eq(null));
        verify(notificationCreationService).create(eq(2L), eq(null), eq(NotificationType.GROUP_ENDING_SOON), any(),
                eq("'모임' 모임의 '모순' 외 1권 읽기가 내일 끝나요."), eq(null), eq(null), eq(null), eq(10L), eq("모임"), eq(null));
    }

    @Test
    @DisplayName("내일 종료되는 책이 있는 모임의 멤버 전원에게 종료 임박 알림을 생성한다")
    void notifiesAllMembersOfGroupsEndingTomorrow() {
        Group group = group(9L);
        given(groupBookEntryRepository.findAllInActiveGroupsByEndDate(LocalDate.now(ZONE).plusDays(1)))
                .willReturn(List.of(entryEndingTomorrow(group)));
        given(groupMemberRepository.findActiveUserIdsByGroupId(9L)).willReturn(List.of(1L, 2L));
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
        given(groupBookEntryRepository.findAllInActiveGroupsByEndDate(LocalDate.now(ZONE).plusDays(1)))
                .willReturn(List.of(entryEndingTomorrow(group)));
        given(groupMemberRepository.findActiveUserIdsByGroupId(9L)).willReturn(List.of(1L));
        given(notificationRepository.existsByReceiverIdAndTypeAndGroupIdAndCreatedAtAfter(
                eq(1L), eq(NotificationType.GROUP_ENDING_SOON), eq(9L), any(LocalDateTime.class)))
                .willReturn(true);

        notifier.notifyGroupsEndingTomorrow();

        verify(notificationCreationService, never()).create(
                eq(1L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("내일 종료되는 책이 없으면 아무 알림도 생성하지 않는다")
    void doesNothingWhenNoGroupsEndingTomorrow() {
        given(groupBookEntryRepository.findAllInActiveGroupsByEndDate(LocalDate.now(ZONE).plusDays(1)))
                .willReturn(List.of());

        notifier.notifyGroupsEndingTomorrow();

        verify(notificationCreationService, never()).create(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("한 모임에서 내일 끝나는 책이 여러 권이어도 멤버에게 알림은 한 번만 생성한다")
    void notifiesOncePerGroupEvenWhenSeveralBooksEndTomorrow() {
        Group group = group(9L);
        given(groupBookEntryRepository.findAllInActiveGroupsByEndDate(LocalDate.now(ZONE).plusDays(1)))
                .willReturn(List.of(entryEndingTomorrow(group), entryEndingTomorrow(group)));
        given(groupMemberRepository.findActiveUserIdsByGroupId(9L)).willReturn(List.of(1L));
        given(notificationRepository.existsByReceiverIdAndTypeAndGroupIdAndCreatedAtAfter(
                any(), eq(NotificationType.GROUP_ENDING_SOON), eq(9L), any(LocalDateTime.class)))
                .willReturn(false);

        notifier.notifyGroupsEndingTomorrow();

        verify(notificationCreationService, times(1)).create(eq(1L), eq(null), eq(NotificationType.GROUP_ENDING_SOON),
                any(), any(), eq(null), eq(null), eq(null), eq(9L), eq("모임"), eq(null));
    }

    @Test
    @DisplayName("모임장이 직접 완독 처리한 책은 종료일이 내일이어도 종료 임박 알림을 만들지 않는다")
    void skipsManuallyFinishedBooks() {
        GroupBookEntry finishedEntry = entryEndingTomorrow(group(9L));
        finishedEntry.changeStatusManually(ReadingStatus.FINISHED);
        given(groupBookEntryRepository.findAllInActiveGroupsByEndDate(LocalDate.now(ZONE).plusDays(1)))
                .willReturn(List.of(finishedEntry));

        notifier.notifyGroupsEndingTomorrow();

        verify(notificationCreationService, never()).create(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
