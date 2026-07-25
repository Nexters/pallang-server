package com.nexters.palang.domain.opinion.presentation.dto;

import com.nexters.palang.domain.decoration.domain.EffectType;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.passage.domain.Passage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateOpinionRequest(
        @NotNull(message = "도서 ID는 필수입니다.")
        Long bookId,

        @Positive(message = "페이지 번호는 1 이상이어야 합니다.")
        int pageNumber,

        @NotBlank(message = "인용 문구는 비어 있을 수 없습니다.")
        @Size(max = Passage.QUOTED_TEXT_MAX_LENGTH, message = "인용 문구는 {max}자를 초과할 수 없습니다.")
        String quotedText,

        boolean isSpoiler,

        // null이면 새 Passage를 생성(신규 또는 유사 문장 병합 거부), 값이 있으면 해당 Passage에 병합한다. (Q-06)
        Long passageId,

        @NotBlank(message = "흔적 내용은 비어 있을 수 없습니다.")
        @Size(max = Opinion.CONTENT_MAX_LENGTH, message = "흔적 내용은 {max}자를 초과할 수 없습니다.")
        String content,

        @NotEmpty(message = "꾸밈 효과는 최소 1개 이상이어야 합니다.")
        @Valid
        List<DecorationRequest> decorations
) {

    public record DecorationRequest(
            @PositiveOrZero(message = "시작 위치는 0 이상이어야 합니다.")
            int startOffset,

            @Positive(message = "끝 위치는 1 이상이어야 합니다.")
            int endOffset,

            @NotNull(message = "효과 종류는 필수입니다.")
            EffectType effectType,

            String color
    ) {
    }
}
