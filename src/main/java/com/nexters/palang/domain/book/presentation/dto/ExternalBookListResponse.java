package com.nexters.palang.domain.book.presentation.dto;

import com.nexters.palang.global.common.response.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ExternalBookListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ExternalBookResponse> books,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageInfo pageInfo
) {
}
