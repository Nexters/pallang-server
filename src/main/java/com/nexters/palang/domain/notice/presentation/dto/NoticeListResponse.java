package com.nexters.palang.domain.notice.presentation.dto;

import com.nexters.palang.global.common.response.PageInfo;
import java.util.List;

public record NoticeListResponse(List<NoticeResponse> notices, PageInfo pageInfo) {
}
