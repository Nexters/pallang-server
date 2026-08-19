package com.nexters.palang.domain.passage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.group.application.GroupAccessValidator;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.passage.infrastructure.PassageQueryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimilarPassageFinderTest {

    @Mock
    private PassageQueryRepository passageQueryRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private GroupAccessValidator groupAccessValidator;

    private SimilarPassageFinder similarPassageFinder;

    @BeforeEach
    void setUp() {
        similarPassageFinder = new SimilarPassageFinder(passageQueryRepository, bookRepository, groupAccessValidator);
    }

    @Test
    @DisplayName("같은 책, 인접 페이지(±1)에 정규화 해시가 같은 대목이 있으면 후보로 반환한다")
    void findReturnsCandidatesWhenSimilarPassageExists() {
        given(bookRepository.existsById(1L)).willReturn(true);
        SimilarPassageProjection projection = new SimilarPassageProjection(10L, "발췌 문장", 5, 2L);
        given(passageQueryRepository.findSimilarCandidates(any(), anyInt(), anyString(), any()))
                .willReturn(List.of(projection));

        List<SimilarPassageProjection> results = similarPassageFinder.find(1L, 5, "발췌 문장", null, 1L);

        assertThat(results).containsExactly(projection);
    }

    @Test
    @DisplayName("존재하지 않는 도서로 조회하면 예외가 발생한다")
    void findThrowsExceptionWhenBookDoesNotExist() {
        given(bookRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> similarPassageFinder.find(1L, 5, "발췌 문장", null, 1L))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("groupId를 지정했을 때 모임원이 아니면 예외가 발생하고 조회는 수행되지 않는다")
    void findFailsWhenNotGroupMember() {
        given(bookRepository.existsById(1L)).willReturn(true);
        doThrow(new GroupException(GroupErrorCode.NOT_MEMBER)).when(groupAccessValidator).validateMember(9L, 1L);

        assertThatThrownBy(() -> similarPassageFinder.find(1L, 5, "발췌 문장", 9L, 1L))
                .isInstanceOf(GroupException.class);
        verify(passageQueryRepository, never()).findSimilarCandidates(any(), anyInt(), anyString(), any());
    }

    @Test
    @DisplayName("groupId를 지정했을 때 모임원이면 그 모임 스코프로 조회한다")
    void findScopesToGroupWhenMember() {
        given(bookRepository.existsById(1L)).willReturn(true);
        given(passageQueryRepository.findSimilarCandidates(any(), anyInt(), anyString(), any())).willReturn(List.of());

        similarPassageFinder.find(1L, 5, "발췌 문장", 9L, 1L);

        verify(groupAccessValidator).validateMember(9L, 1L);
        verify(passageQueryRepository).findSimilarCandidates(1L, 5, PassageNormalizer.normalizedHash("발췌 문장"), 9L);
    }
}
