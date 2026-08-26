package com.nexters.palang.domain.admin.presentation.dto;

import com.nexters.palang.global.common.response.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AdminOpinionListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<AdminOpinionSummaryResponse> opinions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageInfo pageInfo
) {
}
