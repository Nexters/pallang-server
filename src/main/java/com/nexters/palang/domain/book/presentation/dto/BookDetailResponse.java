package com.nexters.palang.domain.book.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexters.palang.domain.book.domain.ReadingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record BookDetailResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long bookId,
        @Schema(example = "채식주의자", requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(example = "한강", requiredMode = Schema.RequiredMode.REQUIRED) String author,
        @Schema(example = "창비", requiredMode = Schema.RequiredMode.REQUIRED) String publisher,
        @Schema(example = "268", requiredMode = Schema.RequiredMode.REQUIRED) int pageCount,
        @Schema(example = "https://image.aladin.co.kr/product/123/45/cover/8936434120_1.jpg", nullable = true) String coverImageUrl,
        @Schema(example = "12", requiredMode = Schema.RequiredMode.REQUIRED) long passageCount,
        @Schema(example = "34", requiredMode = Schema.RequiredMode.REQUIRED) long opinionCount,
        // 비로그인 요청이거나 로그인 사용자가 이 도서에 읽기상태를 남기지 않았으면, null을 내려주는 대신
        // 필드 자체를 응답에서 제외한다(요구사항 명세 기준). 나머지 nullable 필드(coverImageUrl 등)와
        // 직렬화 방식이 달라지므로 클래스 전체가 아니라 이 두 필드에만 개별 적용한다.
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(example = "READING", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED) ReadingStatus myStatus,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(example = "87", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED) Integer myCurrentPage
) {
}
