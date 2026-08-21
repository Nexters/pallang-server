package com.nexters.palang.domain.group.presentation.dto;

import com.nexters.palang.global.common.response.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record GroupListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<GroupSummaryResponse> groups,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageInfo pageInfo
) {
}
