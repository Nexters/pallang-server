package com.nexters.palang.domain.opinion.presentation.dto;

import com.nexters.palang.domain.opinion.application.OpinionSummaryProjection;
import com.nexters.palang.global.common.response.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

public record OpinionListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<OpinionSummaryResponse> opinions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageInfo pageInfo
) {
    public static OpinionListResponse from(Page<OpinionSummaryProjection> page) {
        List<OpinionSummaryResponse> opinions = page.getContent().stream()
                .map(OpinionSummaryResponse::from)
                .toList();
        return new OpinionListResponse(opinions, PageInfo.from(page));
    }
}
