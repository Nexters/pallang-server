package com.nexters.palang.domain.book.presentation.dto;

import com.nexters.palang.global.common.response.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record BookSearchListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<BookSearchResponse> books,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageInfo pageInfo
) {
}
