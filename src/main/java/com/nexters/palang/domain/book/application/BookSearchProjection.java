package com.nexters.palang.domain.book.application;

import com.nexters.palang.domain.book.domain.BookSource;

public record BookSearchProjection(
        Long bookId,
        String title,
        String author,
        String publisher,
        int pageCount,
        String isbn,
        String coverImageUrl,
        BookSource source,
        long passageCount,
        long opinionCount
) {
}
