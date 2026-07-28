package com.nexters.palang.domain.book.presentation.dto;

import com.nexters.palang.domain.book.domain.BookSource;
import io.swagger.v3.oas.annotations.media.Schema;

public record BookSearchResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long bookId,
        @Schema(example = "채식주의자", requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(example = "한강", requiredMode = Schema.RequiredMode.REQUIRED) String author,
        @Schema(example = "창비", requiredMode = Schema.RequiredMode.REQUIRED) String publisher,
        @Schema(example = "268", requiredMode = Schema.RequiredMode.REQUIRED) int pageCount,
        @Schema(example = "9788936434120", nullable = true) String isbn,
        @Schema(example = "https://image.aladin.co.kr/product/123/45/cover/8936434120_1.jpg", nullable = true) String coverImageUrl,
        @Schema(example = "API", requiredMode = Schema.RequiredMode.REQUIRED) BookSource source,
        @Schema(example = "12", requiredMode = Schema.RequiredMode.REQUIRED) long passageCount,
        @Schema(example = "34", requiredMode = Schema.RequiredMode.REQUIRED) long opinionCount
) {
}
