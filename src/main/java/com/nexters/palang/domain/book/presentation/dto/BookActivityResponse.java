package com.nexters.palang.domain.book.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record BookActivityResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long bookId,
        @Schema(example = "채식주의자", requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(example = "한강", requiredMode = Schema.RequiredMode.REQUIRED) String author,
        @Schema(example = "창비", requiredMode = Schema.RequiredMode.REQUIRED) String publisher,
        @Schema(example = "https://image.aladin.co.kr/product/123/45/cover/8936434120_1.jpg", nullable = true) String coverImageUrl,
        @Schema(example = "12", requiredMode = Schema.RequiredMode.REQUIRED) long passageCount,
        @Schema(example = "34", requiredMode = Schema.RequiredMode.REQUIRED) long opinionCount,
        @Schema(example = "8", requiredMode = Schema.RequiredMode.REQUIRED,
                description = "전역 공개 대목(모임 전용이 아닌 대목) 수. passageCount는 모임 전용 대목까지 함께 센다. "
                        + "0이면 모임에서만 읽는 책이라 groupId 없이 대목을 조회하면 빈 목록이다.")
        long publicPassageCount
) {
}
