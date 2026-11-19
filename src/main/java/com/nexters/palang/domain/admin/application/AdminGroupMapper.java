package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.admin.presentation.dto.AdminGroupListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminGroupSummaryResponse;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.global.common.response.PageInfo;
import org.springframework.data.domain.Page;

public final class AdminGroupMapper {

    private AdminGroupMapper() {
    }

    // host는 GroupRepository의 EntityGraph로, 대표 책의 book은 GroupBookEntries의 join fetch로 미리 로딩돼 있다.
    public static AdminGroupSummaryResponse toSummary(AdminGroupSearchResult result) {
        Group group = result.group();
        GroupBookEntry primaryEntry = result.primaryEntry();
        return new AdminGroupSummaryResponse(
                group.getId(), group.getName(), primaryEntry.getBook().getId(), primaryEntry.getBook().getTitle(),
                group.getHost().getId(), group.getHost().getNickname(),
                group.getCapacity(), result.memberCount(), primaryEntry.getStartDate(), primaryEntry.getEndDate(),
                group.getCreatedAt());
    }

    public static AdminGroupListResponse toListResponse(Page<AdminGroupSearchResult> results) {
        return new AdminGroupListResponse(results.map(AdminGroupMapper::toSummary).getContent(), PageInfo.from(results));
    }
}
