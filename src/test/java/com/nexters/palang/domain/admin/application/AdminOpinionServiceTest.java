package com.nexters.palang.domain.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
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

@ExtendWith(MockitoExtension.class)
class AdminOpinionServiceTest {

    @Mock
    private OpinionRepository opinionRepository;
    @Mock
    private PassageRepository passageRepository;
    @Mock
    private AdminCascadeDeleter cascadeDeleter;

    private AdminOpinionService adminOpinionService;

    @BeforeEach
    void setUp() {
        adminOpinionService = new AdminOpinionService(opinionRepository, passageRepository, cascadeDeleter);
    }

    @Test
    @DisplayName("대목 id로 조회하면 그 대목의 의견을 작성 순서대로 가져오도록 리포지토리에 위임한다")
    void searchOpinionsByPassageDelegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Opinion opinion = mock(Opinion.class);
        Page<Opinion> expected = new PageImpl<>(List.of(opinion), pageable, 1);
        given(opinionRepository.findByPassageIdOrderByCreatedAtAscIdAsc(100L, pageable)).willReturn(expected);

        Page<Opinion> results = adminOpinionService.searchOpinionsByPassage(100L, pageable);

        assertThat(results).isEqualTo(expected);
    }

    @Test
    @DisplayName("존재하지 않는 의견을 삭제하려 하면 예외가 발생한다")
    void deleteFailsWhenOpinionNotFound() {
        given(opinionRepository.findWithAssociationsById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminOpinionService.deleteOpinion(1L)).isInstanceOf(OpinionException.class);
    }

    @Test
    @DisplayName("대목에 남은 다른 의견이 없으면 의견 삭제 시 대목도 함께 소프트 삭제된다")
    void deleteOpinionSoftDeletesPassageWhenLast() {
        Passage passage = mock(Passage.class);
        given(passage.getId()).willReturn(100L);
        Opinion opinion = mock(Opinion.class);
        given(opinion.getPassage()).willReturn(passage);
        given(opinionRepository.findWithAssociationsById(1L)).willReturn(Optional.of(opinion));
        given(opinionRepository.existsByPassageIdAndDeletedAtIsNullAndIdNot(100L, 1L)).willReturn(false);
        given(passageRepository.findById(100L)).willReturn(Optional.of(passage));

        adminOpinionService.deleteOpinion(1L);

        verify(cascadeDeleter).deleteOpinions(List.of(1L));
        verify(passage).delete();
    }

    @Test
    @DisplayName("대목에 다른 살아있는 의견이 남아있으면 대목은 삭제되지 않는다")
    void deleteOpinionKeepsPassageWhenOthersRemain() {
        Passage passage = mock(Passage.class);
        given(passage.getId()).willReturn(100L);
        Opinion opinion = mock(Opinion.class);
        given(opinion.getPassage()).willReturn(passage);
        given(opinionRepository.findWithAssociationsById(1L)).willReturn(Optional.of(opinion));
        given(opinionRepository.existsByPassageIdAndDeletedAtIsNullAndIdNot(100L, 1L)).willReturn(true);

        adminOpinionService.deleteOpinion(1L);

        verify(cascadeDeleter).deleteOpinions(List.of(1L));
        verify(passage, never()).delete();
        verify(passageRepository, never()).findById(eq(100L));
    }
}
