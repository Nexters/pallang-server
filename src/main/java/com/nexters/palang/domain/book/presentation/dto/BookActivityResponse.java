package com.nexters.palang.domain.book.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record BookActivityResponse(
        @Schema(example = "1") Long bookId,
        @Schema(example = "채식주의자") String title,
        @Schema(example = "한강") String author,
        @Schema(example = "https://image.aladin.co.kr/product/123/45/cover/8936434120_1.jpg") String coverImageUrl,
        @Schema(example = "12") long passageCount,
        @Schema(example = "34") long opinionCount
) {
}
