package com.nexters.palang.domain.opinion.presentation.dto;

import com.nexters.palang.domain.decoration.domain.EffectType;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.passage.domain.Passage;
import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(example = "1")
        Long bookId,

        @Positive(message = "페이지 번호는 1 이상이어야 합니다.")
        @Schema(example = "87")
        int pageNumber,

        @NotBlank(message = "인용 문구는 비어 있을 수 없습니다.")
        @Size(max = Passage.QUOTED_TEXT_MAX_LENGTH, message = "인용 문구는 {max}자를 초과할 수 없습니다.")
        @Schema(example = "우리는 모두 이야기를 찾아 헤맨다.")
        String quotedText,

        @Schema(example = "false")
        boolean isSpoiler,

        // null이면 새 Passage를 생성(신규 또는 유사 문장 병합 거부), 값이 있으면 해당 Passage에 병합한다. (Q-06)
        @Schema(description = "병합할 기존 대목 ID. 없으면 새 대목을 생성합니다.", example = "5", nullable = true)
        Long passageId,

        // 있으면 이 흔적/대목은 그 모임 전용이 된다(모임원만 조회 가능). 없으면 기존처럼 전역 공개.
        // passageId를 함께 지정한 경우 기존 대목의 소속 모임과 반드시 일치해야 한다(PASSAGE_400_3).
        @Schema(description = "모임 ID. 없으면 전역 공개 흔적/대목으로 생성됩니다.", example = "1", nullable = true)
        Long groupId,

        @NotBlank(message = "흔적 내용은 비어 있을 수 없습니다.")
        @Size(max = Opinion.CONTENT_MAX_LENGTH, message = "흔적 내용은 {max}자를 초과할 수 없습니다.")
        @Schema(example = "이 문장에서 작가의 의도가 느껴져서 좋았어요.")
        String content,

        @NotEmpty(message = "꾸밈 효과는 최소 1개 이상이어야 합니다.")
        @Valid
        List<DecorationRequest> decorations
) {

    public record DecorationRequest(
            @PositiveOrZero(message = "시작 위치는 0 이상이어야 합니다.")
            @Schema(example = "3")
            int startOffset,

            @Positive(message = "끝 위치는 1 이상이어야 합니다.")
            @Schema(example = "12")
            int endOffset,

            @NotNull(message = "효과 종류는 필수입니다.")
            @Schema(example = "HIGHLIGHT")
            EffectType effectType,

            @Schema(example = "#FFE08A")
            String color
    ) {
    }
}
