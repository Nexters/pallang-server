package com.nexters.palang.domain.book.presentation.dto;

import com.nexters.palang.domain.book.domain.BookSource;
import io.swagger.v3.oas.annotations.media.Schema;

public record BookSearchResponse(
        @Schema(example = "1", description = "서비스 DB에 등록된 도서일 때만 채워진다. "
                + "GET /api/books/search에서 알라딘에만 있고 아직 등록되지 않은 도서는 null이다.",
                nullable = true) Long bookId,
        @Schema(example = "채식주의자", requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(example = "한강", requiredMode = Schema.RequiredMode.REQUIRED) String author,
        @Schema(example = "창비", requiredMode = Schema.RequiredMode.REQUIRED) String publisher,
        @Schema(example = "268", description = "GET /api/books/search에서 미등록 도서는 0이다.")
        int pageCount,
        @Schema(example = "9788936434120", nullable = true) String isbn,
        @Schema(example = "https://image.aladin.co.kr/product/123/45/cover/8936434120_1.jpg", nullable = true) String coverImageUrl,
        @Schema(example = "MANUAL", description = "bookId와 마찬가지로 미등록 도서는 null이다.", nullable = true) BookSource source,
        @Schema(example = "12", requiredMode = Schema.RequiredMode.REQUIRED) long passageCount,
        @Schema(example = "34", requiredMode = Schema.RequiredMode.REQUIRED) long opinionCount
) {
}
