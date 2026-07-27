package com.nexters.palang.domain.comment.presentation.dto;

import com.nexters.palang.global.common.response.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CommentListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CommentResponse> comments,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageInfo pageInfo
) {
}
