package com.nexters.palang.domain.user.presentation.dto;

import com.nexters.palang.domain.user.domain.SnsProvider;

public record MeResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String backgroundColor,
        SnsProvider snsProvider,
        long opinionCount
) {
}
