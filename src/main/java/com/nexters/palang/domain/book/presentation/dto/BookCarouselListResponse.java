package com.nexters.palang.domain.book.presentation.dto;

import com.nexters.palang.global.common.response.CarouselPageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record BookCarouselListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<BookActivityResponse> books,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CarouselPageInfo pageInfo
) {
}
