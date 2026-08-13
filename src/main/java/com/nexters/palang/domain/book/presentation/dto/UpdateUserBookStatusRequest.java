package com.nexters.palang.domain.book.presentation.dto;

import com.nexters.palang.domain.book.domain.ReadingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateUserBookStatusRequest(
        @NotNull(message = "도서 ID는 필수입니다.") @Schema(example = "1") Long bookId,
        @NotNull(message = "읽기 상태는 필수입니다.") @Schema(example = "READING") ReadingStatus status,
        @PositiveOrZero(message = "현재 페이지는 0 이상이어야 합니다.") @Schema(example = "87") Integer currentPage
) {
}
