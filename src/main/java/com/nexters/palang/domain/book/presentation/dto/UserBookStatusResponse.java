package com.nexters.palang.domain.book.presentation.dto;

import com.nexters.palang.domain.book.domain.ReadingStatus;
import com.nexters.palang.domain.book.domain.UserBookStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserBookStatusResponse(
        @Schema(example = "1") Long bookId,
        @Schema(example = "READING") ReadingStatus status,
        @Schema(example = "87") Integer currentPage
) {

    public static UserBookStatusResponse from(UserBookStatus userBookStatus) {
        return new UserBookStatusResponse(
                userBookStatus.getBook().getId(),
                userBookStatus.getStatus(),
                userBookStatus.getCurrentPage()
        );
    }
}
