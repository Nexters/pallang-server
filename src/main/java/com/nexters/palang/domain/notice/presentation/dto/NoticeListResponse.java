package com.nexters.palang.domain.notice.presentation.dto;

import com.nexters.palang.global.common.response.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record NoticeListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<NoticeResponse> notices,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageInfo pageInfo
) {
}
