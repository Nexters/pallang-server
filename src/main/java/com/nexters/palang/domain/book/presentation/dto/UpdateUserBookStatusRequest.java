package com.nexters.palang.domain.book.presentation.dto;

import com.nexters.palang.domain.book.domain.ReadingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateUserBookStatusRequest(
        @NotNull(message = "도서 ID는 필수입니다.") Long bookId,
        @NotNull(message = "읽기 상태는 필수입니다.") ReadingStatus status,
        @PositiveOrZero(message = "현재 페이지는 0 이상이어야 합니다.") Integer currentPage
) {
}
