package com.nexters.palang.domain.opinion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.application.BookOptionProjection;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.decoration.domain.Decoration;
import com.nexters.palang.domain.decoration.domain.EffectType;
import com.nexters.palang.domain.group.application.GroupAccessValidator;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.domain.OpinionSortType;
import com.nexters.palang.domain.opinion.infrastructure.OpinionQueryRepository;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.opinion.presentation.dto.CreateOpinionRequest;
import com.nexters.palang.domain.opinion.presentation.dto.CreateOpinionRequest.DecorationRequest;
import com.nexters.palang.domain.opinion.presentation.dto.UpdateOpinionRequest;
import com.nexters.palang.domain.passage.common.error.PassageException;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
class OpinionServiceTest {

    @Mock
    private OpinionRepository opinionRepository;

    @Mock
    private OpinionQueryRepository opinionQueryRepository;

    @Mock
    private PassageRepository passageRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupAccessValidator groupAccessValidator;

    private OpinionService opinionService;

    @BeforeEach
    void setUp() {
        opinionService = new OpinionService(
                opinionRepository, opinionQueryRepository, passageRepository, bookRepository, userRepository,
                groupRepository, groupAccessValidator);
    }

    private User user(Long id) {
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Book book(Long id) {
        Book book = Book.builder().title("제목").author("작가").publisher("출판사").pageCount(300).build();
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }

    private Passage passage(Long id, Book book) {
        Passage passage = Passage.builder().book(book).build();
        ReflectionTestUtils.setField(passage, "id", id);
        return passage;
    }

    private Passage passage(Long id, Book book, Group group) {
        Passage passage = Passage.builder().book(book).group(group).build();
        ReflectionTestUtils.setField(passage, "id", id);
        return passage;
    }

    private Group group(Long id, Book book) {
        Group group = Group.create("모임", book, user(1L), 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    private CreateOpinionRequest request(Long bookId, Long passageId) {
        return request(bookId, passageId, null);
    }

    private CreateOpinionRequest request(Long bookId, Long passageId, Long groupId) {
        return new CreateOpinionRequest(
                bookId, 5, "발췌 문장", false, passageId, groupId, "흔적 내용",
                List.of(new DecorationRequest(0, 5, EffectType.UNDERLINE, null))
        );
    }

    private Opinion opinionOwnedBy(Long opinionId, Long ownerId) {
        Passage passage = passage(100L, book(10L));
        Opinion opinion = Opinion.createWithDecorations(passage, user(ownerId), "흔적 내용",
                List.of(Decoration.builder().startOffset(0).endOffset(5).effectType(EffectType.UNDERLINE).build()));
        ReflectionTestUtils.setField(opinion, "id", opinionId);
        return opinion;
    }

    @Test
    @DisplayName("passageId 없이 흔적을 생성하면 새 Passage가 만들어지고 흔적이 연결된다")
    void createOpinionCreatesNewPassageWhenPassageIdIsNull() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(bookRepository.findById(10L)).willReturn(Optional.of(book(10L)));
        given(passageRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(opinionRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        Opinion opinion = opinionService.createOpinion(1L, request(10L, null));

        assertThat(opinion.getPassage().getBook().getId()).isEqualTo(10L);
        assertThat(opinion.getDecorations()).hasSize(1);
    }

    @Test
    @DisplayName("passageId를 지정해 흔적을 생성하면 기존 Passage에 병합된다")
    void createOpinionMergesIntoExistingPassageWhenPassageIdIsGiven() {
        Book book = book(10L);
        Passage existing = passage(100L, book);
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(passageRepository.findById(100L)).willReturn(Optional.of(existing));
        given(opinionRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        Opinion opinion = opinionService.createOpinion(1L, request(10L, 100L));

        assertThat(opinion.getPassage()).isEqualTo(existing);
    }

    @Test
    @DisplayName("passageId가 삭제된 대목을 가리키면 예외가 발생한다")
    void createOpinionThrowsExceptionWhenPassageIsDeleted() {
        Book book = book(10L);
        Passage deleted = passage(100L, book);
        deleted.delete();
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(passageRepository.findById(100L)).willReturn(Optional.of(deleted));

        assertThatThrownBy(() -> opinionService.createOpinion(1L, request(10L, 100L)))
                .isInstanceOf(PassageException.class);
    }

    @Test
    @DisplayName("passageId가 요청의 bookId와 다른 도서에 속하면 예외가 발생한다")
    void createOpinionThrowsExceptionWhenPassageBelongsToDifferentBook() {
        Book otherBook = book(99L);
        Passage existing = passage(100L, otherBook);
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(passageRepository.findById(100L)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> opinionService.createOpinion(1L, request(10L, 100L)))
                .isInstanceOf(PassageException.class);
    }

    @Test
    @DisplayName("존재하지 않는 도서로 새 Passage를 만들려 하면 예외가 발생한다")
    void createOpinionThrowsExceptionWhenBookDoesNotExist() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(bookRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> opinionService.createOpinion(1L, request(10L, null)))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("groupId를 지정해 새 흔적을 생성하면 대목이 그 모임 소속으로 만들어진다")
    void createOpinionCreatesGroupScopedPassageWhenGroupIdIsGiven() {
        Book book = book(10L);
        Group group = group(9L, book);
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(bookRepository.findById(10L)).willReturn(Optional.of(book));
        given(groupRepository.findById(9L)).willReturn(Optional.of(group));
        given(passageRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(opinionRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        Opinion opinion = opinionService.createOpinion(1L, request(10L, null, 9L));

        assertThat(opinion.getPassage().getGroup()).isEqualTo(group);
        verify(groupAccessValidator).validateMember(9L, 1L);
    }

    @Test
    @DisplayName("모임원이 아닌 사용자가 groupId를 지정해 흔적을 생성하려 하면 예외가 발생한다")
    void createOpinionFailsWhenNotGroupMember() {
        Group group = group(9L, book(10L));
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(groupRepository.findById(9L)).willReturn(Optional.of(group));
        doThrow(new GroupException(GroupErrorCode.NOT_MEMBER)).when(groupAccessValidator).validateMember(9L, 1L);

        assertThatThrownBy(() -> opinionService.createOpinion(1L, request(10L, null, 9L)))
                .isInstanceOf(GroupException.class);
        verify(passageRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 groupId로 흔적을 생성하려 하면 (멤버십이 아니라) 모임을 찾을 수 없다는 예외가 발생한다")
    void createOpinionFailsWhenGroupDoesNotExist() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(groupRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> opinionService.createOpinion(1L, request(10L, null, 999L)))
                .isInstanceOf(GroupException.class)
                .satisfies(e -> assertThat(((GroupException) e).getErrorCode()).isEqualTo(GroupErrorCode.GROUP_NOT_FOUND));
        verify(groupAccessValidator, never()).validateMember(any(), any());
    }

    @Test
    @DisplayName("모임의 지정 도서와 다른 bookId로 모임 전용 흔적을 생성하려 하면 예외가 발생한다")
    void createOpinionFailsWhenGroupBookMismatch() {
        Book otherBook = book(99L);
        Group group = group(9L, book(10L));
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(bookRepository.findById(99L)).willReturn(Optional.of(otherBook));
        given(groupRepository.findById(9L)).willReturn(Optional.of(group));

        assertThatThrownBy(() -> opinionService.createOpinion(1L, request(99L, null, 9L)))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("기존 대목의 소속 모임과 요청의 groupId가 다르면 예외가 발생한다")
    void createOpinionFailsWhenMergePassageGroupMismatch() {
        Book book = book(10L);
        Group group = group(9L, book);
        Passage existingInGroup = passage(100L, book, group);
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(passageRepository.findById(100L)).willReturn(Optional.of(existingInGroup));

        // groupId 없이(전역 공개로 착각하고) 모임 전용 대목에 병합을 시도하는 경우
        assertThatThrownBy(() -> opinionService.createOpinion(1L, request(10L, 100L, null)))
                .isInstanceOf(PassageException.class);
    }

    @Test
    @DisplayName("존재하는 대목으로 흔적 목록을 조회하면 QueryRepository 결과를 그대로 반환한다")
    void getOpinionsReturnsQueryRepositoryResult() {
        given(passageRepository.findByIdAndDeletedAtIsNull(100L)).willReturn(Optional.of(passage(100L, book(10L))));
        Pageable pageable = PageRequest.of(0, 20);

        opinionService.getOpinions(100L, OpinionSortType.LATEST, pageable, 1L);

        verify(opinionQueryRepository).findOpinions(100L, OpinionSortType.LATEST, pageable, 1L);
    }

    @Test
    @DisplayName("존재하지 않는 대목으로 흔적 목록을 조회하면 예외가 발생한다")
    void getOpinionsThrowsExceptionWhenPassageDoesNotExist() {
        given(passageRepository.findByIdAndDeletedAtIsNull(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> opinionService.getOpinions(100L, OpinionSortType.LATEST, PageRequest.of(0, 20), 1L))
                .isInstanceOf(PassageException.class);
    }

    @Test
    @DisplayName("모임 전용 대목의 흔적 목록을 모임원이 아닌 사용자가 조회하면 예외가 발생한다")
    void getOpinionsFailsWhenPassageBelongsToGroupAndNotMember() {
        Group group = group(9L, book(10L));
        given(passageRepository.findByIdAndDeletedAtIsNull(100L)).willReturn(Optional.of(passage(100L, book(10L), group)));
        doThrow(new GroupException(GroupErrorCode.NOT_MEMBER)).when(groupAccessValidator).validateMember(9L, 2L);

        assertThatThrownBy(() -> opinionService.getOpinions(100L, OpinionSortType.LATEST, PageRequest.of(0, 20), 2L))
                .isInstanceOf(GroupException.class);
        verify(opinionQueryRepository, never()).findOpinions(any(), any(), any(), any());
    }

    @Test
    @DisplayName("존재하는 흔적을 조회하면 해당 흔적을 반환한다")
    void getOpinionReturnsExistingOpinion() {
        Opinion opinion = opinionOwnedBy(1L, 1L);
        given(opinionRepository.findDetailById(1L)).willReturn(Optional.of(opinion));

        Opinion result = opinionService.getOpinion(1L, null);

        assertThat(result).isSameAs(opinion);
    }

    @Test
    @DisplayName("삭제된 흔적을 조회하면 예외가 발생한다")
    void getOpinionThrowsExceptionWhenOpinionIsDeleted() {
        Opinion opinion = opinionOwnedBy(1L, 1L);
        opinion.delete();
        given(opinionRepository.findDetailById(1L)).willReturn(Optional.of(opinion));

        assertThatThrownBy(() -> opinionService.getOpinion(1L, null))
                .isInstanceOf(OpinionException.class);
    }

    @Test
    @DisplayName("모임 전용 흔적을 모임원이 아닌 사용자가 조회하면 예외가 발생한다")
    void getOpinionFailsWhenPassageBelongsToGroupAndNotMember() {
        Group group = group(9L, book(10L));
        Passage passage = passage(100L, book(10L), group);
        Opinion opinion = Opinion.createWithDecorations(passage, user(1L), "흔적 내용",
                List.of(Decoration.builder().startOffset(0).endOffset(5).effectType(EffectType.UNDERLINE).build()));
        ReflectionTestUtils.setField(opinion, "id", 1L);
        given(opinionRepository.findDetailById(1L)).willReturn(Optional.of(opinion));
        doThrow(new GroupException(GroupErrorCode.NOT_MEMBER)).when(groupAccessValidator).validateMember(9L, 2L);

        assertThatThrownBy(() -> opinionService.getOpinion(1L, 2L))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("모임 전용 흔적을 모임원이 조회하면 정상적으로 반환된다")
    void getOpinionSucceedsWhenGroupMember() {
        Group group = group(9L, book(10L));
        Passage passage = passage(100L, book(10L), group);
        Opinion opinion = Opinion.createWithDecorations(passage, user(1L), "흔적 내용",
                List.of(Decoration.builder().startOffset(0).endOffset(5).effectType(EffectType.UNDERLINE).build()));
        ReflectionTestUtils.setField(opinion, "id", 1L);
        given(opinionRepository.findDetailById(1L)).willReturn(Optional.of(opinion));

        Opinion result = opinionService.getOpinion(1L, 2L);

        assertThat(result).isSameAs(opinion);
        verify(groupAccessValidator).validateMember(9L, 2L);
    }

    @Test
    @DisplayName("본인이 작성한 흔적을 수정하면 내용이 변경된다")
    void modifyOpinionUpdatesContentWhenOwner() {
        Opinion opinion = opinionOwnedBy(1L, 10L);
        given(opinionRepository.findDetailById(1L)).willReturn(Optional.of(opinion));

        Opinion result = opinionService.modifyOpinion(1L, 10L, new UpdateOpinionRequest("수정된 내용"));

        assertThat(result.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("본인이 아닌 사용자가 흔적을 수정하려 하면 예외가 발생한다")
    void modifyOpinionThrowsExceptionWhenNotOwner() {
        Opinion opinion = opinionOwnedBy(1L, 10L);
        given(opinionRepository.findDetailById(1L)).willReturn(Optional.of(opinion));

        assertThatThrownBy(() -> opinionService.modifyOpinion(1L, 999L, new UpdateOpinionRequest("수정된 내용")))
                .isInstanceOf(OpinionException.class);
    }

    @Test
    @DisplayName("본인이 작성한 흔적을 삭제하면 소프트 삭제된다")
    void removeOpinionDeletesWhenOwner() {
        Opinion opinion = opinionOwnedBy(1L, 10L);
        given(opinionRepository.findDetailById(1L)).willReturn(Optional.of(opinion));
        given(opinionRepository.existsByPassageIdAndDeletedAtIsNullAndIdNot(100L, 1L)).willReturn(true);

        opinionService.removeOpinion(1L, 10L);

        assertThat(opinion.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제 후에도 대목에 다른 살아있는 흔적이 남아있으면 대목은 삭제되지 않는다")
    void removeOpinionKeepsPassageWhenOtherOpinionRemains() {
        Opinion opinion = opinionOwnedBy(1L, 10L);
        given(opinionRepository.findDetailById(1L)).willReturn(Optional.of(opinion));
        given(opinionRepository.existsByPassageIdAndDeletedAtIsNullAndIdNot(100L, 1L)).willReturn(true);

        opinionService.removeOpinion(1L, 10L);

        assertThat(opinion.getPassage().isDeleted()).isFalse();
    }

    @Test
    @DisplayName("삭제 후 대목에 살아있는 흔적이 하나도 남지 않으면 대목도 함께 삭제된다 (PM 요구사항)")
    void removeOpinionDeletesPassageWhenNoOpinionsRemain() {
        Opinion opinion = opinionOwnedBy(1L, 10L);
        given(opinionRepository.findDetailById(1L)).willReturn(Optional.of(opinion));
        given(opinionRepository.existsByPassageIdAndDeletedAtIsNullAndIdNot(100L, 1L)).willReturn(false);

        opinionService.removeOpinion(1L, 10L);

        assertThat(opinion.getPassage().isDeleted()).isTrue();
    }

    @Test
    @DisplayName("본인이 아닌 사용자가 흔적을 삭제하려 하면 예외가 발생한다")
    void removeOpinionThrowsExceptionWhenNotOwner() {
        Opinion opinion = opinionOwnedBy(1L, 10L);
        given(opinionRepository.findDetailById(1L)).willReturn(Optional.of(opinion));

        assertThatThrownBy(() -> opinionService.removeOpinion(1L, 999L))
                .isInstanceOf(OpinionException.class);
    }

    @Test
    @DisplayName("내가 남긴 흔적 목록을 조회하면 QueryRepository 결과를 그대로 반환한다")
    void getMyOpinionsReturnsPageFromQueryRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<MyOpinionProjection> expected = new PageImpl<>(
                List.of(new MyOpinionProjection(1L, 10L, "제목", "작가", "cover", 100L, "발췌", 5, "흔적", 0, LocalDateTime.now())),
                pageable, 1);
        given(opinionQueryRepository.findMyOpinions(1L, null, pageable)).willReturn(expected);

        Page<MyOpinionProjection> results = opinionService.getMyOpinions(1L, null, pageable);

        assertThat(results).isEqualTo(expected);
    }

    @Test
    @DisplayName("비로그인 사용자가 내가 남긴 흔적 목록을 조회하면 QueryRepository 대신 고정 샘플 3건을 반환한다")
    void getMyOpinionsReturnsSampleWhenGuest() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<MyOpinionProjection> results = opinionService.getMyOpinions(null, null, pageable);

        assertThat(results.getTotalElements()).isEqualTo(3);
        assertThat(results.getContent()).allMatch(o -> o.bookId().equals(18L) && o.pageNumber() == 33);
    }

    @Test
    @DisplayName("비로그인 사용자가 다른 책 bookId로 조회하면 샘플이 아닌 빈 목록을 반환한다")
    void getMyOpinionsReturnsEmptyWhenGuestFiltersOtherBook() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<MyOpinionProjection> results = opinionService.getMyOpinions(null, 999L, pageable);

        assertThat(results.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("좋아요 누른 흔적 목록을 조회하면 QueryRepository 결과를 그대로 반환한다")
    void getLikedOpinionsReturnsPageFromQueryRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<LikedOpinionProjection> expected = new PageImpl<>(
                List.of(new LikedOpinionProjection(1L, 10L, "제목", "작가", "cover", 100L, "발췌", 5, "흔적", "닉네임", 0,
                        LocalDateTime.now(), LocalDateTime.now())),
                pageable, 1);
        given(opinionQueryRepository.findLikedOpinions(1L, null, pageable)).willReturn(expected);

        Page<LikedOpinionProjection> results = opinionService.getLikedOpinions(1L, null, pageable);

        assertThat(results).isEqualTo(expected);
    }

    @Test
    @DisplayName("좋아요 도서 필터 목록을 조회하면 QueryRepository 결과를 그대로 반환한다")
    void getLikedBookOptionsReturnsResultFromQueryRepository() {
        List<BookOptionProjection> expected = List.of(new BookOptionProjection(10L, "제목"));
        given(opinionQueryRepository.findLikedBookOptions(1L)).willReturn(expected);

        List<BookOptionProjection> results = opinionService.getLikedBookOptions(1L);

        assertThat(results).isEqualTo(expected);
    }

    @Test
    @DisplayName("내가 남긴 흔적 수를 조회하면 Repository의 카운트를 그대로 반환한다")
    void getMyOpinionCountReturnsRepositoryCount() {
        given(opinionRepository.countByUserIdAndDeletedAtIsNull(1L)).willReturn(3L);

        long count = opinionService.getMyOpinionCount(1L);

        assertThat(count).isEqualTo(3L);
    }
}
