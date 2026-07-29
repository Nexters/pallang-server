package com.nexters.palang.domain.book.application;

import java.util.List;

public record BookCarouselPage(
        List<BookActivityProjection> books,
        long offset,
        int size,
        long totalElements
) {
}
