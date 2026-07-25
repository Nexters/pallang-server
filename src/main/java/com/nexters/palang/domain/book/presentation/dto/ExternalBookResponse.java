package com.nexters.palang.domain.book.presentation.dto;

public record ExternalBookResponse(
        String title,
        String author,
        String publisher,
        String isbn,
        String coverImageUrl
) {
}
