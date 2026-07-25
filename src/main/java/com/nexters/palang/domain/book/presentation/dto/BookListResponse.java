package com.nexters.palang.domain.book.presentation.dto;

import java.util.List;

public record BookListResponse(List<BookResponse> books) {
}
