package com.nexters.palang.domain.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record LikedOpinionResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long opinionId,
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long bookId,
        @Schema(example = "채식주의자", requiredMode = Schema.RequiredMode.REQUIRED) String bookTitle,
        @Schema(example = "한강", requiredMode = Schema.RequiredMode.REQUIRED) String author,
        @Schema(example = "https://image.aladin.co.kr/product/123/45/cover/8936434120_1.jpg", nullable = true) String bookCoverImageUrl,
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long passageId,
        @Schema(example = "우리는 모두 이야기를 찾아 헤맨다.", requiredMode = Schema.RequiredMode.REQUIRED) String quotedText,
        @Schema(example = "87", requiredMode = Schema.RequiredMode.REQUIRED) int pageNumber,
        @Schema(example = "이 문장에서 작가의 의도가 느껴져서 좋았어요.", requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(example = "책벌레", requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
        @Schema(example = "5", requiredMode = Schema.RequiredMode.REQUIRED) int likeCount,
        @Schema(example = "2026-07-20T14:32:00", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt,
        @Schema(example = "2026-07-25T09:10:00", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime likedAt
) {
}
