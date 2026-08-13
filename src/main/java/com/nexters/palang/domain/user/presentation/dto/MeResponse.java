package com.nexters.palang.domain.user.presentation.dto;

import com.nexters.palang.domain.user.domain.SnsProvider;
import io.swagger.v3.oas.annotations.media.Schema;

public record MeResponse(
        @Schema(example = "7", requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
        @Schema(example = "책읽는고양이", requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
        @Schema(example = "reader@example.com", nullable = true) String email,
        @Schema(example = "https://pallang-assets.s3.ap-northeast-2.amazonaws.com/profile/7.png", nullable = true) String profileImageUrl,
        @Schema(example = "#FDF6E3", nullable = true) String backgroundColor,
        @Schema(example = "KAKAO", requiredMode = Schema.RequiredMode.REQUIRED) SnsProvider snsProvider,
        @Schema(example = "23", requiredMode = Schema.RequiredMode.REQUIRED) long opinionCount
) {
}
