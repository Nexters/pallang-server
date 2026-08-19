package com.nexters.palang.domain.notification.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.notification.domain.Notification;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.notification.infrastructure.NotificationRepository;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
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
class BookNewOpinionsNotifierTest {

    private static final int THRESHOLD = 5;

    @Mock
    private OpinionRepository opinionRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private NotificationCreationService notificationCreationService;

    private BookNewOpinionsNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new BookNewOpinionsNotifier(
                opinionRepository, notificationRepository, bookRepository, notificationCreationService, THRESHOLD);
    }

    private Book book(Long id) {
        Book book = Book.builder().title("제목").author("작가").publisher("출판사").pageCount(100).build();
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }

    private Notification notificationWithSnapshot(int snapshot) {
        Notification notification = Notification.builder()
                .type(NotificationType.BOOK_NEW_OPINIONS)
                .title("t").body("b")
                .opinionCountSnapshot(snapshot)
                .build();
        return notification;
    }

    @Test
    @DisplayName("현재 의견 수와 마지막 알림 스냅샷의 차이가 threshold 미만이면 알림을 생성하지 않는다")
    void doesNotNotifyWhenBelowThreshold() {
        given(opinionRepository.countByPassage_Book_IdAndDeletedAtIsNull(1L)).willReturn(4L);
        given(opinionRepository.findDistinctUserIdsByBookId(1L)).willReturn(List.of(10L));
        given(notificationRepository.findFirstByReceiverIdAndTypeAndBookIdOrderByCreatedAtDesc(
                10L, NotificationType.BOOK_NEW_OPINIONS, 1L)).willReturn(Optional.empty());

        notifier.notifyIfThresholdReached(1L, 99L);

        verify(notificationCreationService, never())
                .create(anyLong(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("현재 의견 수가 threshold 이상 누적되면 알림을 생성한다")
    void notifiesWhenThresholdReached() {
        given(opinionRepository.countByPassage_Book_IdAndDeletedAtIsNull(1L)).willReturn(5L);
        given(opinionRepository.findDistinctUserIdsByBookId(1L)).willReturn(List.of(10L));
        given(bookRepository.findById(1L)).willReturn(Optional.of(book(1L)));
        given(notificationRepository.findFirstByReceiverIdAndTypeAndBookIdOrderByCreatedAtDesc(
                10L, NotificationType.BOOK_NEW_OPINIONS, 1L)).willReturn(Optional.empty());

        notifier.notifyIfThresholdReached(1L, 99L);

        verify(notificationCreationService).create(
                eq(10L), eq(null), eq(NotificationType.BOOK_NEW_OPINIONS), any(), any(), eq(null), eq(1L), eq(null), eq(5));
    }

    @Test
    @DisplayName("이미 보낸 알림의 스냅샷 이후 threshold만큼 쌓이지 않았으면 다시 보내지 않는다(중복 방지)")
    void doesNotNotifyAgainBeforeNextThreshold() {
        given(opinionRepository.countByPassage_Book_IdAndDeletedAtIsNull(1L)).willReturn(8L);
        given(opinionRepository.findDistinctUserIdsByBookId(1L)).willReturn(List.of(10L));
        given(notificationRepository.findFirstByReceiverIdAndTypeAndBookIdOrderByCreatedAtDesc(
                10L, NotificationType.BOOK_NEW_OPINIONS, 1L)).willReturn(Optional.of(notificationWithSnapshot(5)));

        notifier.notifyIfThresholdReached(1L, 99L);

        verify(notificationCreationService, never())
                .create(anyLong(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("이미 보낸 알림의 스냅샷 이후 threshold만큼 다시 쌓이면 재알림한다")
    void notifiesAgainOnceNextThresholdReached() {
        given(opinionRepository.countByPassage_Book_IdAndDeletedAtIsNull(1L)).willReturn(10L);
        given(opinionRepository.findDistinctUserIdsByBookId(1L)).willReturn(List.of(10L));
        given(bookRepository.findById(1L)).willReturn(Optional.of(book(1L)));
        given(notificationRepository.findFirstByReceiverIdAndTypeAndBookIdOrderByCreatedAtDesc(
                10L, NotificationType.BOOK_NEW_OPINIONS, 1L)).willReturn(Optional.of(notificationWithSnapshot(5)));

        notifier.notifyIfThresholdReached(1L, 99L);

        verify(notificationCreationService, times(1)).create(
                eq(10L), eq(null), eq(NotificationType.BOOK_NEW_OPINIONS), any(), any(), eq(null), eq(1L), eq(null), eq(10));
    }

    @Test
    @DisplayName("의견을 새로 작성한 사용자 본인은 후보에서 제외한다")
    void excludesActorFromCandidates() {
        given(opinionRepository.countByPassage_Book_IdAndDeletedAtIsNull(1L)).willReturn(5L);
        given(opinionRepository.findDistinctUserIdsByBookId(1L)).willReturn(List.of(10L, 99L));
        given(bookRepository.findById(1L)).willReturn(Optional.of(book(1L)));
        given(notificationRepository.findFirstByReceiverIdAndTypeAndBookIdOrderByCreatedAtDesc(
                10L, NotificationType.BOOK_NEW_OPINIONS, 1L)).willReturn(Optional.empty());

        notifier.notifyIfThresholdReached(1L, 99L);

        verify(notificationCreationService, times(1)).create(
                eq(10L), any(), eq(NotificationType.BOOK_NEW_OPINIONS), any(), any(), any(), eq(1L), any(), any());
        verify(notificationCreationService, never()).create(
                eq(99L), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
