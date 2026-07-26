package com.nexters.palang.domain.user.presentation.dto;

import java.time.LocalDateTime;

public record MyOpinionResponse(
        Long opinionId,
        Long bookId,
        String bookTitle,
        String bookCoverImageUrl,
        Long passageId,
        String quotedText,
        int pageNumber,
        String content,
        int likeCount,
        LocalDateTime createdAt
) {
}
