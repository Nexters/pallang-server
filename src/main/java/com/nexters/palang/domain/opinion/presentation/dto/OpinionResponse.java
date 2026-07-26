package com.nexters.palang.domain.opinion.presentation.dto;

import com.nexters.palang.domain.decoration.domain.Decoration;
import com.nexters.palang.domain.decoration.domain.EffectType;
import com.nexters.palang.domain.opinion.domain.Opinion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record OpinionResponse(
        @Schema(example = "1") Long opinionId,
        @Schema(example = "1") Long passageId,
        @Schema(example = "false") boolean merged,
        @Schema(example = "이 문장에서 작가의 의도가 느껴져서 좋았어요.") String content,
        List<DecorationResponse> decorations,
        @Schema(example = "2026-07-20T14:32:00") LocalDateTime createdAt
) {

    public static OpinionResponse from(Opinion opinion, boolean merged) {
        List<DecorationResponse> decorations = opinion.getDecorations().stream()
                .map(DecorationResponse::from)
                .toList();
        return new OpinionResponse(
                opinion.getId(),
                opinion.getPassage().getId(),
                merged,
                opinion.getContent(),
                decorations,
                opinion.getCreatedAt()
        );
    }

    public record DecorationResponse(
            @Schema(example = "1") Long decorationId,
            @Schema(example = "3") int startOffset,
            @Schema(example = "12") int endOffset,
            @Schema(example = "HIGHLIGHT") EffectType effectType,
            @Schema(example = "#FFE08A") String color
    ) {
        public static DecorationResponse from(Decoration decoration) {
            return new DecorationResponse(
                    decoration.getId(),
                    decoration.getStartOffset(),
                    decoration.getEndOffset(),
                    decoration.getEffectType(),
                    decoration.getColor()
            );
        }
    }
}
