package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.admin.presentation.dto.AdminUserListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminUserSummaryResponse;
import com.nexters.palang.global.common.response.PageInfo;
import org.springframework.data.domain.Page;

public final class AdminUserMapper {

    private AdminUserMapper() {
    }

    public static AdminUserSummaryResponse toSummary(AdminUserSearchResult result) {
        var user = result.user();
        return new AdminUserSummaryResponse(
                user.getId(), user.getNickname(), user.getEmail(), user.getSnsProvider().name(),
                user.isWithdrawn(), user.getCreatedAt(), result.hostedGroupCount());
    }

    public static AdminUserListResponse toListResponse(Page<AdminUserSearchResult> results) {
        return new AdminUserListResponse(
                results.map(AdminUserMapper::toSummary).getContent(), PageInfo.from(results));
    }
}
