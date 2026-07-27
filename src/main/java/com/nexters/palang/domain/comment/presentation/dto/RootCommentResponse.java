package com.nexters.palang.domain.comment.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record RootCommentResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long commentId,
        @Schema(example = "7", requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
        @Schema(example = "책읽는고양이", requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
        @Schema(example = "https://pallang-assets.s3.ap-northeast-2.amazonaws.com/profile/7.png", nullable = true) String profileImageUrl,
        @Schema(example = "저도 같은 생각이에요!", requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(example = "false") boolean isDeleted,
        @Schema(example = "2026-07-20T14:32:00", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt,
        @Schema(example = "2026-07-20T14:32:00", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime updatedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CommentResponse> replies,
        @Schema(example = "8") int replyCount,
        @Schema(example = "true") boolean hasMoreReplies
) {
}
