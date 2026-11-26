package com.nexters.palang.domain.group.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GroupMemberPreviewResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
        @Schema(example = "여백이", requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
        @Schema(example = "https://...", nullable = true, description = "프로필 사진. 없으면 null(앱 기본 이미지 사용)")
        String profileImageUrl
) {
}
