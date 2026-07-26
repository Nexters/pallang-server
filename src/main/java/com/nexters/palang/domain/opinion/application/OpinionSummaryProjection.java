package com.nexters.palang.domain.opinion.application;

import java.time.LocalDateTime;

public record OpinionSummaryProjection(
        Long opinionId,
        Long userId,
        String nickname,
        String content,
        int likeCount,
        LocalDateTime createdAt
) {
}
