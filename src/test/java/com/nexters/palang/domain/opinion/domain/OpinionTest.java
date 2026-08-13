package com.nexters.palang.domain.opinion.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.domain.decoration.common.error.DecorationException;
import com.nexters.palang.domain.decoration.domain.Decoration;
import com.nexters.palang.domain.decoration.domain.EffectType;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.user.domain.User;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpinionTest {

    private Decoration decoration(int startOffset, int endOffset) {
        return Decoration.builder()
                .startOffset(startOffset)
                .endOffset(endOffset)
                .effectType(EffectType.UNDERLINE)
                .build();
    }

    @Test
    @DisplayName("겹치지 않는 꾸밈 효과와 함께 흔적을 생성하면 모든 꾸밈이 연결된다")
    void createWithDecorationsLinksAllDecorations() {
        Passage passage = Passage.builder().build();
        User user = User.builder().build();
        List<Decoration> decorations = List.of(decoration(0, 5), decoration(5, 10));

        Opinion opinion = Opinion.createWithDecorations(passage, user, "흔적 내용", decorations);

        assertThat(opinion.getDecorations()).hasSize(2);
        assertThat(opinion.getDecorations()).allMatch(d -> d.getOpinion() == opinion);
    }

    @Test
    @DisplayName("겹치는 꾸밈 효과로 흔적을 생성하려 하면 예외가 발생하고 흔적이 만들어지지 않는다")
    void createWithDecorationsThrowsExceptionWhenRangesOverlap() {
        Passage passage = Passage.builder().build();
        User user = User.builder().build();
        List<Decoration> decorations = List.of(decoration(0, 10), decoration(5, 15));

        assertThatThrownBy(() -> Opinion.createWithDecorations(passage, user, "흔적 내용", decorations))
                .isInstanceOf(DecorationException.class);
    }
}
