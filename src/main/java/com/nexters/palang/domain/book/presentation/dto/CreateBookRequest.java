package com.nexters.palang.domain.book.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateBookRequest(
        @NotBlank(message = "제목은 필수입니다.") String title,
        @NotBlank(message = "지은이는 필수입니다.") String author,
        @NotBlank(message = "출판사는 필수입니다.") String publisher,
        @Positive(message = "페이지수는 1 이상이어야 합니다.") int pageCount,
        String isbn,
        String coverImageUrl
) {
}
