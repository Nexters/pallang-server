package com.nexters.palang.domain.user.presentation.dto;

import com.nexters.palang.domain.user.domain.SnsProvider;
import io.swagger.v3.oas.annotations.media.Schema;

public record MeResponse(
        @Schema(example = "7") Long userId,
        @Schema(example = "책읽는고양이") String nickname,
        @Schema(example = "https://pallang-assets.s3.ap-northeast-2.amazonaws.com/profile/7.png") String profileImageUrl,
        @Schema(example = "#FDF6E3") String backgroundColor,
        @Schema(example = "KAKAO") SnsProvider snsProvider,
        @Schema(example = "23") long opinionCount
) {
}
