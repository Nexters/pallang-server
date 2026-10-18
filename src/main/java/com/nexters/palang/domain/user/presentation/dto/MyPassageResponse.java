package com.nexters.palang.domain.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record MyPassageResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long passageId,
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long bookId,
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long opinionId,
        @Schema(example = "87", requiredMode = Schema.RequiredMode.REQUIRED) int pageNumber,
        @Schema(example = "우리는 모두 이야기를 찾아 헤맨다.", requiredMode = Schema.RequiredMode.REQUIRED) String quotedText,
        @Schema(example = "false", requiredMode = Schema.RequiredMode.REQUIRED) boolean isSpoiler,
        @Schema(example = "2026-07-20T14:32:00", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt,
        @Schema(description = "모임 전용 대목이면 그 모임 ID, 전역 공개 대목이면 null", example = "13", nullable = true)
        Long groupId,
        @Schema(description = "모임 전용 대목이면 그 모임 이름, 전역 공개 대목이면 null", example = "면도날 같이 읽기", nullable = true)
        String groupName
) {
}
