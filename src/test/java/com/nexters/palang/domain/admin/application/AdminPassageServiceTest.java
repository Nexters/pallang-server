package com.nexters.palang.domain.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.passage.common.error.PassageException;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminPassageServiceTest {

    @Mock
    private PassageRepository passageRepository;
    @Mock
    private AdminCascadeDeleter cascadeDeleter;

    private AdminPassageService adminPassageService;

    @BeforeEach
    void setUp() {
        adminPassageService = new AdminPassageService(passageRepository, cascadeDeleter);
    }

    @Test
    @DisplayName("존재하지 않는 대목을 수정하려 하면 예외가 발생한다")
    void updateFailsWhenPassageNotFound() {
        given(passageRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminPassageService.updatePassage(1L, "내용", 10, false))
                .isInstanceOf(PassageException.class);
    }

    @Test
    @DisplayName("대목을 수정하면 인용문/페이지/스포일러 여부가 바뀐다")
    void updatePassageChangesContent() {
        Passage passage = mock(Passage.class);
        given(passageRepository.findById(1L)).willReturn(Optional.of(passage));

        Passage result = adminPassageService.updatePassage(1L, "새 인용문", 20, true);

        verify(passage).updateContent("새 인용문", 20);
        verify(passage).changeSpoiler(true);
        assertThat(result).isSameAs(passage);
    }

    @Test
    @DisplayName("존재하지 않는 대목을 삭제하려 하면 예외가 발생한다")
    void deleteFailsWhenPassageNotFound() {
        given(passageRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminPassageService.deletePassage(1L)).isInstanceOf(PassageException.class);
    }

    @Test
    @DisplayName("대목을 삭제하면 cascade 삭제기가 호출된다")
    void deletePassageDelegatesToCascadeDeleter() {
        given(passageRepository.findById(1L)).willReturn(Optional.of(mock(Passage.class)));

        adminPassageService.deletePassage(1L);

        verify(cascadeDeleter).deletePassages(List.of(1L));
    }
}
