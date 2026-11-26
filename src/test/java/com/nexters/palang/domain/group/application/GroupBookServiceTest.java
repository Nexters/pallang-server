package com.nexters.palang.domain.group.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.GroupStatus;
import com.nexters.palang.domain.group.domain.ReadingPeriod;
import com.nexters.palang.domain.group.domain.ReadingStatus;
import com.nexters.palang.domain.group.domain.ReadingStatusMode;
import com.nexters.palang.domain.group.domain.event.GroupScheduleChangedEvent;
import com.nexters.palang.domain.group.infrastructure.GroupBookEntryRepository;
import com.nexters.palang.domain.group.infrastructure.GroupBookQueryRepository;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.group.presentation.dto.AddGroupBookRequest;
import com.nexters.palang.domain.group.presentation.dto.UpdateGroupBookRequest;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.idempotency.IdempotentExecutor;
import com.nexters.palang.global.idempotency.IdempotentOperation;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GroupBookServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(ZonedDateTime.of(2026, 11, 20, 10, 0, 0, 0, SEOUL).toInstant(), SEOUL);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private GroupBookEntryRepository groupBookEntryRepository;
    @Mock
    private GroupBookQueryRepository groupBookQueryRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private IdempotentExecutor idempotentExecutor;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GroupBookService service;
    private User host;
    private Group group;

    @BeforeEach
    void setUp() {
        service = new GroupBookService(groupRepository, groupMemberRepository, groupBookEntryRepository,
                groupBookQueryRepository, bookRepository, idempotentExecutor, eventPublisher, CLOCK);
        host = User.builder().nickname("호스트").build();
        ReflectionTestUtils.setField(host, "id", 1L);
        group = Group.create("고전 뽀개기", host, 4);
        ReflectionTestUtils.setField(group, "id", 9L);
    }

    private Book book(Long id, String isbn) {
        Book built = Book.builder().title("책" + id).author("작가").publisher("출판사").pageCount(100).isbn(isbn)
                .source(BookSource.API).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private GroupBookEntry entry(Long id, Book book, ReadingPeriod period) {
        GroupBookEntry built = GroupBookEntry.create(group, book, period, LocalDate.MIN);
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private void noRecordCounts() {
        given(groupBookQueryRepository.countPassagesByBook(eq(9L), anyCollection())).willReturn(Map.of());
        given(groupBookQueryRepository.countOpinionsByBook(eq(9L), anyCollection(), any())).willReturn(Map.of());
    }

    @SuppressWarnings("unchecked")
    private void runIdempotentActionDirectly() {
        given(idempotentExecutor.execute(anyLong(), eq(IdempotentOperation.ADD_GROUP_BOOK), any(), any()))
                .willAnswer(invocation -> ((Supplier<Long>) invocation.getArgument(3)).get());
    }

    // ===== 목록 =====

    @Test
    @DisplayName("현재 멤버가 아니면 모임 책 목록을 볼 수 없다")
    void getGroupBooksFailsWhenNotMember() {
        given(groupRepository.findByIdWithHost(9L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsActiveMember(9L, 2L)).willReturn(false);

        assertThatThrownBy(() -> service.getGroupBooks(9L, 2L, null, PAGEABLE))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.NOT_MEMBER);
    }

    @Test
    @DisplayName("합쳐진 모임의 책 목록은 볼 수 없다")
    void getGroupBooksFailsWhenGroupMerged() {
        ReflectionTestUtils.setField(group, "status", GroupStatus.MERGED);
        given(groupRepository.findByIdWithHost(9L)).willReturn(Optional.of(group));

        assertThatThrownBy(() -> service.getGroupBooks(9L, 1L, null, PAGEABLE))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.GROUP_NOT_FOUND);
    }

    @Test
    @DisplayName("범위를 지정하지 않으면 함께 읽는 책 탭을 기본으로 보여준다")
    void defaultsToReadingTab() {
        given(groupRepository.findByIdWithHost(9L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsActiveMember(9L, 1L)).willReturn(true);
        given(groupBookQueryRepository.countBooks(9L, ReadingStatus.READING, TODAY)).willReturn(2L);
        given(groupBookQueryRepository.countBooks(9L, ReadingStatus.FINISHED, TODAY)).willReturn(1L);
        given(groupBookQueryRepository.findBooks(9L, ReadingStatus.READING, TODAY, PAGEABLE))
                .willReturn(new PageImpl<>(List.of(), PAGEABLE, 0));
        noRecordCounts();

        GroupBookPage page = service.getGroupBooks(9L, 1L, null, PAGEABLE);

        assertThat(page.statusFilter()).isEqualTo(GroupBookStatusFilter.READING);
        assertThat(page.readingCount()).isEqualTo(2);
        assertThat(page.finishedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("함께 읽는 책이 0권이면 완독한 책 탭을 기본으로 보여준다")
    void defaultsToFinishedTabWhenNoReadingBooks() {
        given(groupRepository.findByIdWithHost(9L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsActiveMember(9L, 1L)).willReturn(true);
        given(groupBookQueryRepository.countBooks(9L, ReadingStatus.READING, TODAY)).willReturn(0L);
        given(groupBookQueryRepository.countBooks(9L, ReadingStatus.FINISHED, TODAY)).willReturn(3L);
        given(groupBookQueryRepository.findBooks(9L, ReadingStatus.FINISHED, TODAY, PAGEABLE))
                .willReturn(new PageImpl<>(List.of(), PAGEABLE, 0));
        noRecordCounts();

        assertThat(service.getGroupBooks(9L, 1L, null, PAGEABLE).statusFilter()).isEqualTo(GroupBookStatusFilter.FINISHED);
    }

    @Test
    @DisplayName("책 카드에는 조회 시점 상태와 문장/의견 수가 담긴다")
    void viewIncludesStatusAndRecordCounts() {
        Book book = book(100L, null);
        GroupBookEntry ended = entry(10L, book, ReadingPeriod.of(TODAY.minusDays(10), TODAY.minusDays(1)));
        given(groupRepository.findByIdWithHost(9L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsActiveMember(9L, 1L)).willReturn(true);
        given(groupBookQueryRepository.findBooks(9L, null, TODAY, PAGEABLE))
                .willReturn(new PageImpl<>(List.of(ended), PAGEABLE, 1));
        given(groupBookQueryRepository.countPassagesByBook(9L, List.of(100L))).willReturn(Map.of(100L, 6L));
        given(groupBookQueryRepository.countOpinionsByBook(9L, List.of(100L), 1L)).willReturn(Map.of(100L, 17L));

        GroupBookView view = service.getGroupBooks(9L, 1L, GroupBookStatusFilter.ALL, PAGEABLE).books().getContent().getFirst();

        assertThat(view.status()).isEqualTo(ReadingStatus.FINISHED);
        assertThat(view.passageCount()).isEqualTo(6);
        assertThat(view.opinionCount()).isEqualTo(17);
    }

    // ===== 추가 =====

    @Test
    @DisplayName("모임장이 아니면 책을 추가할 수 없다")
    void addFailsWhenNotHost() {
        runIdempotentActionDirectly();
        given(groupRepository.findByIdForUpdate(9L)).willReturn(Optional.of(group));

        assertThatThrownBy(() -> service.addGroupBook(9L, 2L, new AddGroupBookRequest(100L, null, null), null))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.NOT_HOST);
        verify(groupBookEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 판본(같은 ISBN)이 이미 등록돼 있으면 추가할 수 없다")
    void addFailsWhenSameEditionAlreadyAdded() {
        runIdempotentActionDirectly();
        given(groupRepository.findByIdForUpdate(9L)).willReturn(Optional.of(group));
        given(bookRepository.findById(200L)).willReturn(Optional.of(book(200L, "9788936434127")));
        GroupBookEntry finishedSameEdition = entry(10L, book(100L, "9788936434127"),
                ReadingPeriod.of(TODAY.minusDays(10), TODAY.minusDays(1)));
        given(groupBookEntryRepository.findAllByGroupIdForUpdate(9L)).willReturn(List.of(finishedSameEdition));

        assertThatThrownBy(() -> service.addGroupBook(9L, 1L, new AddGroupBookRequest(200L, null, null), null))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.GROUP_BOOK_ALREADY_ADDED);
        verify(groupBookEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("모임장은 새 판본을 기간과 함께 추가할 수 있고, 같은 요청 키로 처리된다")
    void addSucceeds() {
        runIdempotentActionDirectly();
        Book newBook = book(200L, null);
        given(groupRepository.findByIdForUpdate(9L)).willReturn(Optional.of(group));
        given(bookRepository.findById(200L)).willReturn(Optional.of(newBook));
        given(groupBookEntryRepository.findAllByGroupIdForUpdate(9L))
                .willReturn(List.of(entry(10L, book(100L, null), ReadingPeriod.NONE)));
        AtomicReference<GroupBookEntry> savedEntry = new AtomicReference<>();
        given(groupBookEntryRepository.save(any(GroupBookEntry.class))).willAnswer(invocation -> {
            GroupBookEntry saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 11L);
            savedEntry.set(saved);
            return saved;
        });
        given(groupBookEntryRepository.findWithBookByIdAndGroupId(11L, 9L))
                .willAnswer(invocation -> Optional.of(savedEntry.get()));
        noRecordCounts();

        GroupBookView view = service.addGroupBook(
                9L, 1L, new AddGroupBookRequest(200L, TODAY, TODAY.plusDays(30)), "add-key");

        assertThat(view.entry().getId()).isEqualTo(11L);
        assertThat(view.entry().getPeriod()).isEqualTo(ReadingPeriod.of(TODAY, TODAY.plusDays(30)));
        assertThat(view.status()).isEqualTo(ReadingStatus.READING);
        verify(idempotentExecutor).execute(eq(1L), eq(IdempotentOperation.ADD_GROUP_BOOK), eq("add-key"), any());
    }

    @Test
    @DisplayName("과거 날짜로는 책을 추가할 수 없다")
    void addFailsWithPastDate() {
        runIdempotentActionDirectly();
        given(groupRepository.findByIdForUpdate(9L)).willReturn(Optional.of(group));
        given(bookRepository.findById(200L)).willReturn(Optional.of(book(200L, null)));
        given(groupBookEntryRepository.findAllByGroupIdForUpdate(9L)).willReturn(List.of());

        assertThatThrownBy(() -> service.addGroupBook(
                9L, 1L, new AddGroupBookRequest(200L, TODAY.minusDays(1), TODAY.plusDays(3)), null))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.PAST_GROUP_DATE);
        verify(groupBookEntryRepository, never()).save(any());
    }

    // ===== 설정 변경 =====

    @Test
    @DisplayName("모임장이 아니면 책 설정을 바꿀 수 없다")
    void updateFailsWhenNotHost() {
        given(groupRepository.findByIdWithHost(9L)).willReturn(Optional.of(group));

        assertThatThrownBy(() -> service.updateGroupBook(9L, 10L, 2L, new UpdateGroupBookRequest(null, null, null)))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.NOT_HOST);
    }

    @Test
    @DisplayName("이 모임의 책이 아니면 설정을 바꿀 수 없다")
    void updateFailsWhenEntryNotInGroup() {
        given(groupRepository.findByIdWithHost(9L)).willReturn(Optional.of(group));
        given(groupBookEntryRepository.findWithBookByIdAndGroupId(99L, 9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateGroupBook(9L, 99L, 1L, new UpdateGroupBookRequest(null, null, null)))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.GROUP_BOOK_ENTRY_NOT_FOUND);
    }

    @Test
    @DisplayName("기간을 바꾸면 저장되고 일정 변경 알림 이벤트가 발행된다")
    void updatePeriodPublishesScheduleChangedEvent() {
        GroupBookEntry entry = entry(10L, book(100L, null), ReadingPeriod.of(TODAY, TODAY.plusDays(10)));
        given(groupRepository.findByIdWithHost(9L)).willReturn(Optional.of(group));
        given(groupBookEntryRepository.findWithBookByIdAndGroupId(10L, 9L)).willReturn(Optional.of(entry));
        noRecordCounts();

        service.updateGroupBook(9L, 10L, 1L, new UpdateGroupBookRequest(TODAY, TODAY.plusDays(20), null));

        assertThat(entry.getEndDate()).isEqualTo(TODAY.plusDays(20));
        assertThat(entry.getStatusMode()).isEqualTo(ReadingStatusMode.AUTO);
        verify(eventPublisher).publishEvent(new GroupScheduleChangedEvent(9L, "고전 뽀개기", 1L));
    }

    @Test
    @DisplayName("과거 기간은 그대로 두고 상태만 완독으로 바꿀 수 있으며, 이때 일정 변경 알림은 없다")
    void updateStatusOnlyKeepsPastPeriod() {
        ReadingPeriod pastPeriod = ReadingPeriod.of(TODAY.minusDays(30), TODAY.minusDays(1));
        GroupBookEntry entry = entry(10L, book(100L, null), pastPeriod);
        given(groupRepository.findByIdWithHost(9L)).willReturn(Optional.of(group));
        given(groupBookEntryRepository.findWithBookByIdAndGroupId(10L, 9L)).willReturn(Optional.of(entry));
        noRecordCounts();

        GroupBookView view = service.updateGroupBook(9L, 10L, 1L,
                new UpdateGroupBookRequest(pastPeriod.startDate(), pastPeriod.endDate(), ReadingStatus.READING));

        assertThat(view.status()).isEqualTo(ReadingStatus.READING);
        assertThat(entry.getStatusMode()).isEqualTo(ReadingStatusMode.MANUAL);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("기간을 새 과거 날짜로 바꾸면 예외가 발생한다")
    void updateFailsWithNewPastDate() {
        GroupBookEntry entry = entry(10L, book(100L, null), ReadingPeriod.of(TODAY, TODAY.plusDays(10)));
        given(groupRepository.findByIdWithHost(9L)).willReturn(Optional.of(group));
        given(groupBookEntryRepository.findWithBookByIdAndGroupId(10L, 9L)).willReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.updateGroupBook(9L, 10L, 1L,
                new UpdateGroupBookRequest(TODAY.minusDays(1), TODAY.plusDays(10), null)))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.PAST_GROUP_DATE);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
