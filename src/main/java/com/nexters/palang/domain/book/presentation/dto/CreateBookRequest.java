package com.nexters.palang.domain.book.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateBookRequest(
        @NotBlank(message = "제목은 필수입니다.") @Schema(example = "채식주의자") String title,
        @NotBlank(message = "지은이는 필수입니다.") @Schema(example = "한강") String author,
        @NotBlank(message = "출판사는 필수입니다.") @Schema(example = "창비") String publisher,
        @Positive(message = "페이지수는 1 이상이어야 합니다.") @Schema(example = "268") int pageCount,
        @Schema(example = "9788936434120") String isbn
) {
}
