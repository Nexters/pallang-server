package com.nexters.palang.domain.block.presentation.dto;

import com.nexters.palang.domain.block.domain.UserBlock;
import com.nexters.palang.global.common.response.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

public record BlockedUserListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<BlockedUserResponse> users,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageInfo pageInfo
) {
    public static BlockedUserListResponse from(Page<UserBlock> page) {
        List<BlockedUserResponse> users = page.getContent().stream()
                .map(BlockedUserResponse::from)
                .toList();
        return new BlockedUserListResponse(users, PageInfo.from(page));
    }
}
