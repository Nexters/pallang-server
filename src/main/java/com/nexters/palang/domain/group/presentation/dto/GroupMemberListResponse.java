package com.nexters.palang.domain.group.presentation.dto;

import com.nexters.palang.global.common.response.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record GroupMemberListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<GroupMemberResponse> members,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageInfo pageInfo,
        @Schema(example = "5", description = "현재 멤버 수(명단 제목의 \"5 / 10명\"). 페이지 크기와 무관한 전체 기준",
                requiredMode = Schema.RequiredMode.REQUIRED) long memberCount,
        @Schema(example = "10", description = "모임 정원", requiredMode = Schema.RequiredMode.REQUIRED) int capacity
) {
}
