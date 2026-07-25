package com.nexters.palang.domain.decoration.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.domain.decoration.common.error.DecorationException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DecorationRangeValidatorTest {

    private Decoration decoration(int startOffset, int endOffset) {
        return Decoration.builder()
                .startOffset(startOffset)
                .endOffset(endOffset)
                .effectType(EffectType.UNDERLINE)
                .build();
    }

    @Test
    @DisplayName("겹치지 않는 꾸밈 효과 목록은 예외 없이 검증을 통과한다")
    void validateSucceedsWhenRangesDoNotOverlap() {
        List<Decoration> decorations = List.of(decoration(0, 5), decoration(5, 10), decoration(10, 15));

        assertThatCode(() -> DecorationRangeValidator.validate(decorations)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("두 꾸밈 효과의 영역이 겹치면 예외가 발생한다")
    void validateThrowsExceptionWhenRangesOverlap() {
        List<Decoration> decorations = List.of(decoration(0, 10), decoration(5, 15));

        assertThatThrownBy(() -> DecorationRangeValidator.validate(decorations))
                .isInstanceOf(DecorationException.class);
    }

    @Test
    @DisplayName("끝 위치가 시작 위치보다 작거나 같으면 예외가 발생한다")
    void validateThrowsExceptionWhenEndOffsetIsNotGreaterThanStartOffset() {
        List<Decoration> decorations = List.of(decoration(5, 5));

        assertThatThrownBy(() -> DecorationRangeValidator.validate(decorations))
                .isInstanceOf(DecorationException.class);
    }
}
