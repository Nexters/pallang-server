package com.nexters.palang.domain.passage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

import com.nexters.palang.domain.book.application.BookOptionProjection;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.decoration.application.DecorationMergeCandidate;
import com.nexters.palang.domain.decoration.domain.EffectType;
import com.nexters.palang.domain.decoration.infrastructure.DecorationQueryRepository;
import com.nexters.palang.domain.group.application.GroupAccessValidator;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.passage.common.error.PassageException;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageQueryRepository;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
class PassageServiceTest {

    @Mock
    private PassageQueryRepository passageQueryRepository;

    @Mock
    private DecorationQueryRepository decorationQueryRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private GroupAccessValidator groupAccessValidator;

    @Mock
    private PassageRepository passageRepository;

    @Mock
    private OpinionRepository opinionRepository;

    private PassageService passageService;

    @BeforeEach
    void setUp() {
        passageService = new PassageService(
                passageQueryRepository, decorationQueryRepository, bookRepository,
                groupAccessValidator, passageRepository, opinionRepository);
    }

    private Passage passage(Long id) {
        Passage passage = Passage.builder().build();
        ReflectionTestUtils.setField(passage, "id", id);
        return passage;
    }

    @Test
    @DisplayName("존재하지 않는 도서로 페이지 번호를 조회하면 예외가 발생한다")
    void getPageNumbersThrowsExceptionWhenBookDoesNotExist() {
        given(bookRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> passageService.getPageNumbers(1L, null, 1L, PageRequest.of(0, 20)))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("존재하지 않는 도서로 페이지의 대목을 조회하면 예외가 발생한다")
    void getPassagesByPageThrowsExceptionWhenBookDoesNotExist() {
        given(bookRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> passageService.getPassagesByPage(1L, null, 1L, 3))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("페이지에 대목이 없으면 예외 없이 빈 결과가 조회된다")
    void getPassagesByPageReturnsEmptyWhenNoPassagesExist() {
        given(bookRepository.existsById(1L)).willReturn(true);
        given(passageQueryRepository.findPassagesByPage(1L, null, 3)).willReturn(List.of());

        List<Passage> result = passageService.getPassagesByPage(1L, null, 1L, 3);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("스포일러 대목을 포함해 해당 페이지의 모든 대목을 조회한다")
    void getPassagesByPageReturnsAllPassagesIncludingSpoilers() {
        given(bookRepository.existsById(1L)).willReturn(true);
        given(passageQueryRepository.findPassagesByPage(1L, null, 3)).willReturn(List.of(passage(100L)));

        List<Passage> result = passageService.getPassagesByPage(1L, null, 1L, 3);

        assertThat(result).extracting(Passage::getId).containsExactly(100L);
    }

    @Test
    @DisplayName("groupId를 지정해 페이지 번호를 조회할 때 모임원이 아니면 예외가 발생한다")
    void getPageNumbersFailsWhenNotGroupMember() {
        given(bookRepository.findById(1L)).willReturn(Optional.of(
                Book.builder().title("제목").author("작가").publisher("출판사").pageCount(300).build()));
        doThrow(new GroupException(GroupErrorCode.NOT_MEMBER)).when(groupAccessValidator).validateMember(9L, 1L);

        assertThatThrownBy(() -> passageService.getPageNumbers(1L, 9L, 1L, PageRequest.of(0, 20)))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("groupId를 지정해 페이지의 대목을 조회할 때 모임원이 아니면 예외가 발생한다")
    void getPassagesByPageFailsWhenNotGroupMember() {
        given(bookRepository.existsById(1L)).willReturn(true);
        doThrow(new GroupException(GroupErrorCode.NOT_MEMBER)).when(groupAccessValidator).validateMember(9L, 1L);

        assertThatThrownBy(() -> passageService.getPassagesByPage(1L, 9L, 1L, 3))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("각 대목의 꾸밈 병합 결과를 대목 ID별로 모은다")
    void getMergedDecorationsByPassageIdGroupsResultsByPassageId() {
        Passage passage = passage(1L);
        DecorationMergeCandidate candidate = new DecorationMergeCandidate(
                10L, 0, 5, EffectType.UNDERLINE, "#PRIMARY", 3, LocalDateTime.now());
        given(decorationQueryRepository.findMergeCandidates(1L)).willReturn(List.of(candidate));

        Map<Long, List<DecorationMergeCandidate>> result =
                passageService.getMergedDecorationsByPassageId(List.of(passage));

        assertThat(result.get(1L)).containsExactly(candidate);
    }

    @Test
    @DisplayName("스포일러 도서 필터 목록을 조회하면 QueryRepository 결과를 그대로 반환한다")
    void getSpoilerBookOptionsReturnsResultFromQueryRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<BookOptionProjection> expected = new PageImpl<>(List.of(new BookOptionProjection(10L, "책 제목")));
        given(passageQueryRepository.findSpoilerBookOptions(1L, pageable)).willReturn(expected);

        Page<BookOptionProjection> result = passageService.getSpoilerBookOptions(1L, pageable);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("이 대목에 흔적을 남긴 사용자는 스포일러 설정을 변경할 수 있다")
    void updateSpoilerChangesFlagWhenUserHasOpinionOnPassage() {
        Passage passage = passage(1L);
        given(passageRepository.findById(1L)).willReturn(Optional.of(passage));
        given(opinionRepository.existsByPassageIdAndUserIdAndDeletedAtIsNull(1L, 10L)).willReturn(true);

        Passage result = passageService.updateSpoiler(1L, 10L, true);

        assertThat(result.isSpoiler()).isTrue();
    }

    @Test
    @DisplayName("스포일러 해제 후 다시 켜는 재설정도 허용한다")
    void updateSpoilerAllowsResettingFalseToTrue() {
        Passage passage = passage(1L);
        ReflectionTestUtils.setField(passage, "isSpoiler", false);
        given(passageRepository.findById(1L)).willReturn(Optional.of(passage));
        given(opinionRepository.existsByPassageIdAndUserIdAndDeletedAtIsNull(1L, 10L)).willReturn(true);

        Passage result = passageService.updateSpoiler(1L, 10L, true);

        assertThat(result.isSpoiler()).isTrue();
    }

    @Test
    @DisplayName("이 대목에 흔적을 남긴 적 없는 사용자가 변경을 시도하면 예외가 발생한다")
    void updateSpoilerThrowsExceptionWhenUserHasNoOpinionOnPassage() {
        Passage passage = passage(1L);
        given(passageRepository.findById(1L)).willReturn(Optional.of(passage));
        given(opinionRepository.existsByPassageIdAndUserIdAndDeletedAtIsNull(1L, 999L)).willReturn(false);

        assertThatThrownBy(() -> passageService.updateSpoiler(1L, 999L, true))
                .isInstanceOf(PassageException.class);
    }

    @Test
    @DisplayName("존재하지 않는 대목의 스포일러 설정을 변경하면 예외가 발생한다")
    void updateSpoilerThrowsExceptionWhenPassageDoesNotExist() {
        given(passageRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> passageService.updateSpoiler(1L, 10L, true))
                .isInstanceOf(PassageException.class);
    }

    @Test
    @DisplayName("삭제된 대목의 스포일러 설정을 변경하면 예외가 발생한다")
    void updateSpoilerThrowsExceptionWhenPassageDeleted() {
        Passage passage = passage(1L);
        passage.delete();
        given(passageRepository.findById(1L)).willReturn(Optional.of(passage));

        assertThatThrownBy(() -> passageService.updateSpoiler(1L, 10L, true))
                .isInstanceOf(PassageException.class);
    }
}
