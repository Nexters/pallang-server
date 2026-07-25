package com.nexters.palang.domain.book.presentation.dto;

import com.nexters.palang.domain.book.domain.ReadingStatus;
import com.nexters.palang.domain.book.domain.UserBookStatus;

public record UserBookStatusResponse(Long bookId, ReadingStatus status, Integer currentPage) {

    public static UserBookStatusResponse from(UserBookStatus userBookStatus) {
        return new UserBookStatusResponse(
                userBookStatus.getBook().getId(),
                userBookStatus.getStatus(),
                userBookStatus.getCurrentPage()
        );
    }
}
