package com.nexters.palang.domain.passage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.decoration.application.DecorationMergeCandidate;
import com.nexters.palang.domain.decoration.domain.EffectType;
import com.nexters.palang.domain.decoration.infrastructure.DecorationQueryRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageQueryRepository;
import com.nexters.palang.global.security.LoginRequiredException;
import com.querydsl.core.types.dsl.Expressions;
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
    private PassageVisibilityFilter passageVisibilityFilter;

    @Mock
    private BookRepository bookRepository;

    private PassageService passageService;

    @BeforeEach
    void setUp() {
        passageService = new PassageService(
                passageQueryRepository, decorationQueryRepository, passageVisibilityFilter, bookRepository);
    }

    private Passage passage(Long id) {
        Passage passage = Passage.builder().build();
        ReflectionTestUtils.setField(passage, "id", id);
        return passage;
    }

    @Test
    @DisplayName("존재하지 않는 도서로 페이지 번호를 조회하면 예외가 발생한다")
    void getVisiblePageNumbersThrowsExceptionWhenBookDoesNotExist() {
        given(bookRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> passageService.getVisiblePageNumbers(1L, 10L, PageRequest.of(0, 20)))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("비로그인 사용자가 첫 노출 페이지가 아닌 페이지를 요청하면 예외가 발생한다")
    void getVisiblePassagesByPageThrowsExceptionWhenAnonymousRequestsNonFirstPage() {
        given(bookRepository.existsById(1L)).willReturn(true);
        given(passageVisibilityFilter.firstVisiblePageNumber(1L)).willReturn(Optional.of(3));

        assertThatThrownBy(() -> passageService.getVisiblePassagesByPage(1L, 5, null))
                .isInstanceOf(LoginRequiredException.class);
    }

    @Test
    @DisplayName("비로그인 사용자가 첫 노출 페이지를 요청하면 정상 조회된다")
    void getVisiblePassagesByPageSucceedsWhenAnonymousRequestsFirstPage() {
        given(bookRepository.existsById(1L)).willReturn(true);
        given(passageVisibilityFilter.firstVisiblePageNumber(1L)).willReturn(Optional.of(3));
        given(passageVisibilityFilter.build(1L, null)).willReturn(Expressions.asBoolean(true).isTrue());
        given(passageQueryRepository.findVisiblePassagesByPage(any(), anyInt(), any())).willReturn(List.of(passage(100L)));

        List<Passage> result = passageService.getVisiblePassagesByPage(1L, 3, null);

        assertThat(result).extracting(Passage::getId).containsExactly(100L);
    }

    @Test
    @DisplayName("노출 가능한 대목이 하나도 없는 도서에 비로그인 사용자가 접근하면 예외 없이 빈 결과가 조회된다")
    void getVisiblePassagesByPageDoesNotThrowWhenNoPassagesExistForAnonymous() {
        given(bookRepository.existsById(1L)).willReturn(true);
        given(passageVisibilityFilter.firstVisiblePageNumber(1L)).willReturn(Optional.empty());
        given(passageVisibilityFilter.build(1L, null)).willReturn(Expressions.asBoolean(false).isTrue());
        given(passageQueryRepository.findVisiblePassagesByPage(any(), anyInt(), any())).willReturn(List.of());

        List<Passage> result = passageService.getVisiblePassagesByPage(1L, 3, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("로그인 사용자는 노출 범위 밖의 페이지를 요청해도 예외 없이 빈 결과가 조회된다")
    void getVisiblePassagesByPageDoesNotThrowForLoggedInUser() {
        given(bookRepository.existsById(1L)).willReturn(true);
        given(passageVisibilityFilter.build(1L, 10L)).willReturn(Expressions.asBoolean(false).isTrue());
        given(passageQueryRepository.findVisiblePassagesByPage(any(), anyInt(), any())).willReturn(List.of());

        List<Passage> result = passageService.getVisiblePassagesByPage(1L, 99, 10L);

        assertThat(result).isEmpty();
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
}
