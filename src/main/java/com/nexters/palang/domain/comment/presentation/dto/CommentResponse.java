package com.nexters.palang.domain.comment.presentation.dto;

import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
        Long userId,
        String nickname,
        String profileImageUrl,
        String content,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
