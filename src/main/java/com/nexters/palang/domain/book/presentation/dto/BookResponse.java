package com.nexters.palang.domain.book.presentation.dto;

import com.nexters.palang.domain.book.domain.BookSource;

public record BookResponse(
        Long bookId,
        String title,
        String author,
        String publisher,
        int pageCount,
        String isbn,
        String coverImageUrl,
        BookSource source
) {
}
