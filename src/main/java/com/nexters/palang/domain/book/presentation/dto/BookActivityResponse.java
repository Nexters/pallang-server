package com.nexters.palang.domain.book.presentation.dto;

public record BookActivityResponse(
        Long bookId,
        String title,
        String author,
        String coverImageUrl,
        long passageCount,
        long opinionCount
) {
}
