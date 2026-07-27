package com.nexters.palang.domain.book.presentation.dto;

import com.nexters.palang.domain.book.domain.ReadingStatus;
import com.nexters.palang.domain.book.domain.UserBookStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserBookStatusResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long bookId,
        @Schema(example = "READING", requiredMode = Schema.RequiredMode.REQUIRED) ReadingStatus status,
        @Schema(example = "87", nullable = true) Integer currentPage
) {

    public static UserBookStatusResponse from(UserBookStatus userBookStatus) {
        return new UserBookStatusResponse(
                userBookStatus.getBook().getId(),
                userBookStatus.getStatus(),
                userBookStatus.getCurrentPage()
        );
    }
}
