package com.nexters.palang.domain.book.presentation.dto;

import com.nexters.palang.global.common.response.PageInfo;
import java.util.List;

public record BookListResponse(List<BookResponse> books, PageInfo pageInfo) {
}
