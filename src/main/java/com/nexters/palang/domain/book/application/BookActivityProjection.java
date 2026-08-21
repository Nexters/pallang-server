package com.nexters.palang.domain.book.application;

public record BookActivityProjection(
        Long bookId,
        String title,
        String author,
        String publisher,
        String coverImageUrl,
        long passageCount,
        long opinionCount
) {
}
