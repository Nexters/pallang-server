package com.nexters.palang.domain.comment.presentation.dto;

import com.nexters.palang.domain.comment.domain.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Size(max = Comment.CONTENT_MAX_LENGTH, message = "댓글은 500자를 초과할 수 없습니다.")
        @Schema(example = "다시 읽어보니 더 공감돼요.")
        String content
) {
}
