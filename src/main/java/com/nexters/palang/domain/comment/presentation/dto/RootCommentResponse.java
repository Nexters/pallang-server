package com.nexters.palang.domain.comment.presentation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RootCommentResponse(
        Long commentId,
        Long userId,
        String nickname,
        String profileImageUrl,
        String content,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentResponse> replies,
        int replyCount,
        boolean hasMoreReplies
) {
}
