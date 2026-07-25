package com.nexters.palang.domain.opinion.presentation.dto;

import com.nexters.palang.domain.opinion.application.OpinionSummaryProjection;
import com.nexters.palang.global.common.response.PageInfo;
import java.util.List;
import org.springframework.data.domain.Page;

public record OpinionListResponse(List<OpinionSummaryResponse> opinions, PageInfo pageInfo) {
    public static OpinionListResponse from(Page<OpinionSummaryProjection> page) {
        List<OpinionSummaryResponse> opinions = page.getContent().stream()
                .map(OpinionSummaryResponse::from)
                .toList();
        return new OpinionListResponse(opinions, PageInfo.from(page));
    }
}
