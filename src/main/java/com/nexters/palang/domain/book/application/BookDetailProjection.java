package com.nexters.palang.domain.book.application;

public record BookDetailProjection(
        Long bookId,
        String title,
        String author,
        String publisher,
        int pageCount,
        String coverImageUrl,
        long passageCount,
        long opinionCount
) {
}
