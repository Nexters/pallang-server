package com.nexters.palang.domain.opinion.presentation.dto;

import com.nexters.palang.domain.decoration.domain.Decoration;
import com.nexters.palang.domain.decoration.domain.EffectType;
import com.nexters.palang.domain.opinion.domain.Opinion;
import java.time.LocalDateTime;
import java.util.List;

public record OpinionResponse(
        Long opinionId,
        Long passageId,
        boolean merged,
        String content,
        List<DecorationResponse> decorations,
        LocalDateTime createdAt
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

    public record DecorationResponse(Long decorationId, int startOffset, int endOffset, EffectType effectType, String color) {
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
