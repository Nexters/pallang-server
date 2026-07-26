package com.nexters.palang.domain.notice.presentation.dto;

import java.time.LocalDateTime;

public record NoticeResponse(
        Long noticeId,
        String title,
        String content,
        LocalDateTime createdAt
) {
}
