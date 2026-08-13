package com.nexters.palang.domain.opinion.presentation.dto;

import com.nexters.palang.domain.opinion.application.OpinionSummaryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record OpinionSummaryResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long opinionId,
        @Schema(example = "7", requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
        @Schema(example = "책읽는고양이", requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
        @Schema(example = "이 문장에서 작가의 의도가 느껴져서 좋았어요.", requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(example = "5", requiredMode = Schema.RequiredMode.REQUIRED) int likeCount,
        @Schema(example = "2026-07-20T14:32:00", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt,
        @Schema(example = "true", requiredMode = Schema.RequiredMode.REQUIRED) boolean liked,
        @Schema(example = "3", requiredMode = Schema.RequiredMode.REQUIRED) long commentCount
) {
    public static OpinionSummaryResponse from(OpinionSummaryProjection projection) {
        return new OpinionSummaryResponse(
                projection.opinionId(),
                projection.userId(),
                projection.nickname(),
                projection.content(),
                projection.likeCount(),
                projection.createdAt(),
                projection.liked(),
                projection.commentCount()
        );
    }
}
