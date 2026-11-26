package com.nexters.palang.domain.group.presentation.dto;

import com.nexters.palang.domain.group.application.GroupBookStatusFilter;
import com.nexters.palang.global.common.response.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record GroupBookListResponse(
        @Schema(example = "READING", description = "이번 응답에 적용한 범위. 요청에 status가 없으면 서버가 고른 기본 탭"
                + "(함께 읽는 책, 0권이면 완독한 책)", requiredMode = Schema.RequiredMode.REQUIRED)
        GroupBookStatusFilter status,
        @Schema(example = "2", description = "함께 읽는 책 수(탭 표시용, 범위와 무관한 모임 전체 기준)",
                requiredMode = Schema.RequiredMode.REQUIRED) long readingCount,
        @Schema(example = "0", description = "완독한 책 수(탭 표시용, 범위와 무관한 모임 전체 기준)",
                requiredMode = Schema.RequiredMode.REQUIRED) long finishedCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<GroupBookResponse> books,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageInfo pageInfo
) {
}
