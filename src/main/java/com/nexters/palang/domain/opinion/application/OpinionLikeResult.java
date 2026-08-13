package com.nexters.palang.domain.opinion.application;

public record OpinionLikeResult(Long opinionId, boolean liked, int likeCount) {
}
