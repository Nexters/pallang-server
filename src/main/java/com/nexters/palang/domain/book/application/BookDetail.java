package com.nexters.palang.domain.book.application;

import com.nexters.palang.domain.book.domain.UserBookStatus;

public record BookDetail(
        BookDetailProjection book,
        UserBookStatus myStatus
) {
}
