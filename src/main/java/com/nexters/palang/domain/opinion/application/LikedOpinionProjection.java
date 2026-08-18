package com.nexters.palang.domain.opinion.application;

import java.time.LocalDateTime;

public record LikedOpinionProjection(
        Long opinionId,
        Long bookId,
        String bookTitle,
        String author,
        String bookCoverImageUrl,
        Long passageId,
        String quotedText,
        int pageNumber,
        String content,
        String nickname,
        int likeCount,
        LocalDateTime createdAt,
        LocalDateTime likedAt
) {
}
