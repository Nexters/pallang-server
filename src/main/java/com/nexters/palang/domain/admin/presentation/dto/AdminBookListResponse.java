package com.nexters.palang.domain.admin.presentation.dto;

import com.nexters.palang.global.common.response.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AdminBookListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<AdminBookSummaryResponse> books,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageInfo pageInfo
) {
}
