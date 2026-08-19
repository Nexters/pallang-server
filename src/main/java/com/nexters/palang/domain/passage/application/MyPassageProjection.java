package com.nexters.palang.domain.passage.application;

import java.time.LocalDateTime;

public record MyPassageProjection(
        Long passageId,
        Long bookId,
        Long opinionId,
        int pageNumber,
        String quotedText,
        boolean isSpoiler,
        LocalDateTime createdAt
) {
}
