package com.nexters.palang.domain.opinion.presentation.dto;

import com.nexters.palang.domain.opinion.application.OpinionLikeResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record OpinionLikeResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long opinionId,
        @Schema(example = "true") boolean liked,
        @Schema(example = "42") int likeCount
) {

    public static OpinionLikeResponse from(OpinionLikeResult result) {
        return new OpinionLikeResponse(result.opinionId(), result.liked(), result.likeCount());
    }
}
