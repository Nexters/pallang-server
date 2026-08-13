package com.nexters.palang.domain.block.presentation.dto;

import com.nexters.palang.domain.block.domain.UserBlock;
import com.nexters.palang.domain.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record BlockedUserResponse(
        @Schema(example = "7", requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
        @Schema(example = "책읽는고양이", requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
        @Schema(example = "https://pallang-assets.s3.ap-northeast-2.amazonaws.com/profile/7.png", nullable = true) String profileImageUrl,
        @Schema(example = "2026-08-05T14:32:00", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime blockedAt
) {
    public static BlockedUserResponse from(UserBlock userBlock) {
        User blocked = userBlock.getBlocked();
        return new BlockedUserResponse(
                blocked.getId(), blocked.getNickname(), blocked.getProfileImageUrl(), userBlock.getCreatedAt());
    }
}
