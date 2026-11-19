package com.nexters.palang.domain.group.application;

import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.ReadingPeriod;
import com.nexters.palang.domain.group.domain.ReadingStatus;
import com.nexters.palang.domain.group.domain.event.GroupScheduleChangedEvent;
import com.nexters.palang.domain.group.infrastructure.GroupBookEntryRepository;
import com.nexters.palang.domain.group.infrastructure.GroupBookQueryRepository;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.group.presentation.dto.AddGroupBookRequest;
import com.nexters.palang.domain.group.presentation.dto.UpdateGroupBookRequest;
import com.nexters.palang.global.idempotency.IdempotentExecutor;
import com.nexters.palang.global.idempotency.IdempotentOperation;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 모임 책(GroupBookEntry) 목록/추가/설정. 모임 자체(이름/정원/멤버/초대)는 GroupService가 맡는다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupBookService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupBookEntryRepository groupBookEntryRepository;
    private final GroupBookQueryRepository groupBookQueryRepository;
    private final BookRepository bookRepository;
    private final IdempotentExecutor idempotentExecutor;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // 현재 멤버만 조회할 수 있다. statusFilter가 없으면 기본 탭(함께 읽는 책, 0권이면 완독한 책)을 고른다(L08).
    // "조회 실패"와 "0권"을 구분할 수 있도록 0권이어도 빈 목록과 개수를 정상 응답한다.
    public GroupBookPage getGroupBooks(Long groupId, Long userId, GroupBookStatusFilter statusFilter, Pageable pageable) {
        getActiveGroup(groupId);
        validateMember(groupId, userId);
        LocalDate today = today();

        long readingCount = groupBookQueryRepository.countBooks(groupId, ReadingStatus.READING, today);
        long finishedCount = groupBookQueryRepository.countBooks(groupId, ReadingStatus.FINISHED, today);
        GroupBookStatusFilter appliedFilter = statusFilter != null ? statusFilter : defaultFilter(readingCount, finishedCount);

        Page<GroupBookEntry> entries =
                groupBookQueryRepository.findBooks(groupId, appliedFilter.toReadingStatus(), today, pageable);
        List<GroupBookView> views = toViews(groupId, userId, entries.getContent(), today);
        return new GroupBookPage(appliedFilter, readingCount, finishedCount,
                new PageImpl<>(views, entries.getPageable(), entries.getTotalElements()));
    }

    private GroupBookStatusFilter defaultFilter(long readingCount, long finishedCount) {
        return readingCount == 0 && finishedCount > 0 ? GroupBookStatusFilter.FINISHED : GroupBookStatusFilter.READING;
    }

    // 모임장만 책을 추가할 수 있다. 실제 entry는 이 요청에서만 만든다(B01/B02). 같은 판본이 이미 있으면(읽는 중이든
    // 완독이든) 막는다(B04). 두 기기에서 같은 판본을 동시에 추가해도 하나만 성공하도록 모임 행을 잠근 채로 중복을
    // 확인한다(B05) — 합치기로 같은 판본 entry가 여럿 생길 수 있어 DB 유니크 제약 대신 잠금을 쓴다. 정원이 가득 차도
    // 책 추가는 막지 않는다. 요청 키가 같으면 처음 추가한 결과를 돌려준다(B07). 트랜잭션은 IdempotentExecutor가 연다
    // (GroupService.createGroup과 같은 이유).
    @Transactional(propagation = Propagation.SUPPORTS)
    public GroupBookView addGroupBook(Long groupId, Long userId, AddGroupBookRequest request, String requestKey) {
        Long entryId = idempotentExecutor.execute(
                userId, IdempotentOperation.ADD_GROUP_BOOK, requestKey, () -> insertGroupBook(groupId, userId, request));
        GroupBookEntry entry = groupBookEntryRepository.findWithBookByIdAndGroupId(entryId, groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_BOOK_ENTRY_NOT_FOUND));
        return toView(groupId, userId, entry, today());
    }

    private Long insertGroupBook(Long groupId, Long userId, AddGroupBookRequest request) {
        Group group = groupRepository.findByIdForUpdate(groupId)
                .filter(Group::isActive)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        validateHost(group, userId);
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));
        boolean alreadyAdded = groupBookEntryRepository.findAllWithBookByGroupIdIn(List.of(groupId)).stream()
                .anyMatch(existing -> existing.isSameEditionAs(book));
        if (alreadyAdded) {
            throw new GroupException(GroupErrorCode.GROUP_BOOK_ALREADY_ADDED);
        }
        ReadingPeriod period = ReadingPeriod.of(request.startDate(), request.endDate());
        return groupBookEntryRepository.save(GroupBookEntry.create(group, book, period, today())).getId();
    }

    // 모임장만 바꿀 수 있다. 판본은 바꿀 수 없고 기간과 상태만 바꾼다(F06). 기간은 요청 값으로 통째로 바꾸며
    // (둘 다 비우면 기간 없음), 상태는 클라이언트가 직접 고른 경우에만 보내고 그때만 수동 모드로 바뀐다.
    // 기간이 실제로 바뀌면 다른 멤버에게 일정 변경 알림을 보낸다.
    @Transactional
    public GroupBookView updateGroupBook(Long groupId, Long entryId, Long userId, UpdateGroupBookRequest request) {
        Group group = getActiveGroup(groupId);
        validateHost(group, userId);
        GroupBookEntry entry = groupBookEntryRepository.findWithBookByIdAndGroupId(entryId, groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_BOOK_ENTRY_NOT_FOUND));

        ReadingPeriod newPeriod = ReadingPeriod.of(request.startDate(), request.endDate());
        boolean periodChanged = !newPeriod.equals(entry.getPeriod());
        entry.changeSettings(newPeriod, request.status(), today());
        if (periodChanged) {
            eventPublisher.publishEvent(new GroupScheduleChangedEvent(group.getId(), group.getName(), userId));
        }
        return toView(groupId, userId, entry, today());
    }

    private GroupBookView toView(Long groupId, Long viewerId, GroupBookEntry entry, LocalDate today) {
        return toViews(groupId, viewerId, List.of(entry), today).getFirst();
    }

    // 기록 수는 지금 (모임, 책) 단위로 쌓인다(Passage.group/book). 합치기(#227)로 같은 판본 entry가 한 모임에 여럿
    // 생기면 entry 단위 연결이 필요하다.
    private List<GroupBookView> toViews(Long groupId, Long viewerId, List<GroupBookEntry> entries, LocalDate today) {
        List<Long> bookIds = entries.stream().map(entry -> entry.getBook().getId()).distinct().toList();
        Map<Long, Long> passageCounts = groupBookQueryRepository.countPassagesByBook(groupId, bookIds);
        Map<Long, Long> opinionCounts = groupBookQueryRepository.countOpinionsByBook(groupId, bookIds, viewerId);
        return entries.stream()
                .map(entry -> new GroupBookView(entry, entry.currentStatus(today),
                        passageCounts.getOrDefault(entry.getBook().getId(), 0L),
                        opinionCounts.getOrDefault(entry.getBook().getId(), 0L)))
                .toList();
    }

    private Group getActiveGroup(Long groupId) {
        return groupRepository.findByIdWithHost(groupId)
                .filter(Group::isActive)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
    }

    private void validateHost(Group group, Long userId) {
        if (!group.isHost(userId)) {
            throw new GroupException(GroupErrorCode.NOT_HOST);
        }
    }

    private void validateMember(Long groupId, Long userId) {
        if (!groupMemberRepository.existsActiveMember(groupId, userId)) {
            throw new GroupException(GroupErrorCode.NOT_MEMBER);
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
