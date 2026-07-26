package com.nexters.palang.domain.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record MyOpinionResponse(
        @Schema(example = "1") Long opinionId,
        @Schema(example = "1") Long bookId,
        @Schema(example = "채식주의자") String bookTitle,
        @Schema(example = "https://image.aladin.co.kr/product/123/45/cover/8936434120_1.jpg") String bookCoverImageUrl,
        @Schema(example = "1") Long passageId,
        @Schema(example = "우리는 모두 이야기를 찾아 헤맨다.") String quotedText,
        @Schema(example = "87") int pageNumber,
        @Schema(example = "이 문장에서 작가의 의도가 느껴져서 좋았어요.") String content,
        @Schema(example = "5") int likeCount,
        @Schema(example = "2026-07-20T14:32:00") LocalDateTime createdAt
) {
}
