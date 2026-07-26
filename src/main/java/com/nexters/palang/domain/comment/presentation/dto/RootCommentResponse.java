package com.nexters.palang.domain.comment.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record RootCommentResponse(
        @Schema(example = "1") Long commentId,
        @Schema(example = "7") Long userId,
        @Schema(example = "책읽는고양이") String nickname,
        @Schema(example = "https://pallang-assets.s3.ap-northeast-2.amazonaws.com/profile/7.png") String profileImageUrl,
        @Schema(example = "저도 같은 생각이에요!") String content,
        @Schema(example = "false") boolean isDeleted,
        @Schema(example = "2026-07-20T14:32:00") LocalDateTime createdAt,
        @Schema(example = "2026-07-20T14:32:00") LocalDateTime updatedAt,
        List<CommentResponse> replies,
        @Schema(example = "8") int replyCount,
        @Schema(example = "true") boolean hasMoreReplies
) {
}
