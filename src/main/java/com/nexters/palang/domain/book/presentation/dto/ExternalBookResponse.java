package com.nexters.palang.domain.book.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ExternalBookResponse(
        @Schema(example = "채식주의자") String title,
        @Schema(example = "한강") String author,
        @Schema(example = "창비") String publisher,
        @Schema(description = "응답 속도를 위해 채우지 않으며 항상 null입니다.", nullable = true) Integer pageCount,
        @Schema(example = "9788936434120") String isbn,
        @Schema(example = "https://image.aladin.co.kr/product/123/45/cover/8936434120_1.jpg") String coverImageUrl
) {
}
