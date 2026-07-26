package com.nexters.palang.domain.opinion.presentation.dto;

import com.nexters.palang.domain.opinion.application.OpinionSummaryProjection;
import java.time.LocalDateTime;

public record OpinionSummaryResponse(
        Long opinionId,
        Long userId,
        String nickname,
        String content,
        int likeCount,
        LocalDateTime createdAt
) {
    public static OpinionSummaryResponse from(OpinionSummaryProjection projection) {
        return new OpinionSummaryResponse(
                projection.opinionId(),
                projection.userId(),
                projection.nickname(),
                projection.content(),
                projection.likeCount(),
                projection.createdAt()
        );
    }
}
