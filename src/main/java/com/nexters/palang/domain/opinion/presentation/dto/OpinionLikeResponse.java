package com.nexters.palang.domain.opinion.presentation.dto;

import com.nexters.palang.domain.opinion.application.OpinionLikeResult;

public record OpinionLikeResponse(Long opinionId, boolean liked, int likeCount) {

    public static OpinionLikeResponse from(OpinionLikeResult result) {
        return new OpinionLikeResponse(result.opinionId(), result.liked(), result.likeCount());
    }
}
