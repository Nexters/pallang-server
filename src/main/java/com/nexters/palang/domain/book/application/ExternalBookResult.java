package com.nexters.palang.domain.book.application;

public record ExternalBookResult(
        String title,
        String author,
        String publisher,
        String isbn,
        String coverImageUrl
) {
}
