package com.nexters.palang.domain.passage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.nexters.palang.domain.book.application.BookOptionProjection;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.decoration.application.DecorationMergeCandidate;
import com.nexters.palang.domain.decoration.domain.EffectType;
import com.nexters.palang.domain.decoration.infrastructure.DecorationQueryRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageQueryRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PassageServiceTest {

    @Mock
    private PassageQueryRepository passageQueryRepository;

    @Mock
    private DecorationQueryRepository decorationQueryRepository;

    @Mock
    private BookRepository bookRepository;

    private PassageService passageService;

    @BeforeEach
    void setUp() {
        passageService = new PassageService(passageQueryRepository, decorationQueryRepository, bookRepository);
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

        assertThatThrownBy(() -> passageService.getPageNumbers(1L, PageRequest.of(0, 20)))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("존재하지 않는 도서로 페이지의 대목을 조회하면 예외가 발생한다")
    void getPassagesByPageThrowsExceptionWhenBookDoesNotExist() {
        given(bookRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> passageService.getPassagesByPage(1L, 3))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("페이지에 대목이 없으면 예외 없이 빈 결과가 조회된다")
    void getPassagesByPageReturnsEmptyWhenNoPassagesExist() {
        given(bookRepository.existsById(1L)).willReturn(true);
        given(passageQueryRepository.findPassagesByPage(1L, 3)).willReturn(List.of());

        List<Passage> result = passageService.getPassagesByPage(1L, 3);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("스포일러 대목을 포함해 해당 페이지의 모든 대목을 조회한다")
    void getPassagesByPageReturnsAllPassagesIncludingSpoilers() {
        given(bookRepository.existsById(1L)).willReturn(true);
        given(passageQueryRepository.findPassagesByPage(1L, 3)).willReturn(List.of(passage(100L)));

        List<Passage> result = passageService.getPassagesByPage(1L, 3);

        assertThat(result).extracting(Passage::getId).containsExactly(100L);
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
        List<BookOptionProjection> expected = List.of(new BookOptionProjection(10L, "책 제목"));
        given(passageQueryRepository.findSpoilerBookOptions(1L)).willReturn(expected);

        List<BookOptionProjection> result = passageService.getSpoilerBookOptions(1L);

        assertThat(result).isEqualTo(expected);
    }
}
