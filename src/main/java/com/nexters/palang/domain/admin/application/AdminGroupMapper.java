package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.admin.presentation.dto.AdminGroupListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminGroupSummaryResponse;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.global.common.response.PageInfo;
import org.springframework.data.domain.Page;

public final class AdminGroupMapper {

    private AdminGroupMapper() {
    }

    // book/host는 지연 로딩 연관관계다. AdminPassageMapper와 같은 이유로 open-in-view에 기대고 있다.
    public static AdminGroupSummaryResponse toSummary(AdminGroupSearchResult result) {
        Group group = result.group();
        return new AdminGroupSummaryResponse(
                group.getId(), group.getName(), group.getBook().getId(), group.getBook().getTitle(),
                group.getHost().getId(), group.getHost().getNickname(),
                group.getCapacity(), result.memberCount(), group.getStartDate(), group.getEndDate(), group.getCreatedAt());
    }

    public static AdminGroupListResponse toListResponse(Page<AdminGroupSearchResult> results) {
        return new AdminGroupListResponse(results.map(AdminGroupMapper::toSummary).getContent(), PageInfo.from(results));
    }
}
