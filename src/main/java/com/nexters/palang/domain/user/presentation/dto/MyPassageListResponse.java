package com.nexters.palang.domain.user.presentation.dto;

import com.nexters.palang.global.common.response.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MyPassageListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<MyPassageResponse> passages,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageInfo pageInfo
) {
}
