package com.nexters.palang.domain.comment.presentation.dto;

import com.nexters.palang.global.common.response.PageInfo;
import java.util.List;

public record RootCommentListResponse(List<RootCommentResponse> comments, PageInfo pageInfo) {
}
