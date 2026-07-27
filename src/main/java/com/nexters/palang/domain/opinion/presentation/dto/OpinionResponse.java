package com.nexters.palang.domain.opinion.presentation.dto;

import com.nexters.palang.domain.decoration.domain.Decoration;
import com.nexters.palang.domain.decoration.domain.EffectType;
import com.nexters.palang.domain.opinion.domain.Opinion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record OpinionResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long opinionId,
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long passageId,
        @Schema(example = "false") boolean merged,
        @Schema(example = "이 문장에서 작가의 의도가 느껴져서 좋았어요.", requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<DecorationResponse> decorations,
        @Schema(example = "2026-07-20T14:32:00", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
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
            @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long decorationId,
            @Schema(example = "3") int startOffset,
            @Schema(example = "12") int endOffset,
            @Schema(example = "HIGHLIGHT", requiredMode = Schema.RequiredMode.REQUIRED) EffectType effectType,
            @Schema(example = "#FFE08A", requiredMode = Schema.RequiredMode.REQUIRED) String color
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
