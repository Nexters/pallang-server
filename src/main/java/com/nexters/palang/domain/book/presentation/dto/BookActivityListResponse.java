package com.nexters.palang.domain.book.presentation.dto;

import com.nexters.palang.global.common.response.PageInfo;
import java.util.List;

public record BookActivityListResponse(List<BookActivityResponse> books, PageInfo pageInfo) {
}
