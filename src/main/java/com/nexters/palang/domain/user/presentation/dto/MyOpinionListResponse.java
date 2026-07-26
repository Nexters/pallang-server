package com.nexters.palang.domain.user.presentation.dto;

import com.nexters.palang.global.common.response.PageInfo;
import java.util.List;

public record MyOpinionListResponse(List<MyOpinionResponse> opinions, PageInfo pageInfo) {
}
