package com.nexters.palang.domain.comment.presentation.dto;

import com.nexters.palang.domain.comment.domain.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @Schema(description = "답글로 남길 경우 원댓글 ID. 원댓글이면 null입니다.", example = "3", nullable = true)
        Long parentCommentId,
        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Size(max = Comment.CONTENT_MAX_LENGTH, message = "댓글은 500자를 초과할 수 없습니다.")
        @Schema(example = "저도 같은 생각이에요!")
        String content
) {
}
