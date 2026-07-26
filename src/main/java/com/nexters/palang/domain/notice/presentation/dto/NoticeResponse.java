package com.nexters.palang.domain.notice.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record NoticeResponse(
        @Schema(example = "1") Long noticeId,
        @Schema(example = "서비스 업데이트 안내") String title,
        @Schema(example = "이용에 참고 부탁드립니다. 자세한 내용은 아래를 확인해주세요.") String content,
        @Schema(example = "2026-07-20T10:15:30") LocalDateTime createdAt
) {
}
