package com.nexters.palang.domain.group.presentation.dto;

import com.nexters.palang.domain.group.domain.GroupMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record GroupMemberResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
        @Schema(example = "여백이", requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
        @Schema(example = "https://...", nullable = true) String profileImageUrl,
        @Schema(example = "HOST", requiredMode = Schema.RequiredMode.REQUIRED) GroupMemberRole role,
        @Schema(example = "2026-08-19T12:00:00", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime joinedAt,
        @Schema(example = "true", description = "모임장 여부(계정 ID로 판별). 본인이 모임장이면 isMe와 함께 true",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean isHost,
        @Schema(example = "false", description = "조회한 본인 여부(계정 ID로 판별)",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean isMe
) {
}
