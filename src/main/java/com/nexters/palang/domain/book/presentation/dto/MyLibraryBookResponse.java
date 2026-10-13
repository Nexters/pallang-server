package com.nexters.palang.domain.book.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MyLibraryBookResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long bookId,
        @Schema(example = "채식주의자", requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(example = "한강", requiredMode = Schema.RequiredMode.REQUIRED) String author,
        @Schema(example = "창비", requiredMode = Schema.RequiredMode.REQUIRED) String publisher,
        @Schema(example = "https://image.aladin.co.kr/product/123/45/cover/8936434120_1.jpg", nullable = true) String coverImageUrl,
        @Schema(description = "모임 전용 항목이면 그 모임 ID, 전역 공개 항목이면 null", example = "13", nullable = true)
        Long groupId,
        @Schema(description = "모임 전용 항목이면 그 모임 이름, 전역 공개 항목이면 null", example = "면도날 같이 읽기", nullable = true)
        String groupName,
        @Schema(description = "이 항목 범위(전역 또는 해당 모임)의 대목 수", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        long passageCount,
        @Schema(description = "이 항목 범위(전역 또는 해당 모임)의 흔적 수", example = "34", requiredMode = Schema.RequiredMode.REQUIRED)
        long opinionCount
) {
}
