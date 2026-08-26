package com.nexters.palang.domain.admin.presentation.dto;

import com.nexters.palang.domain.comment.domain.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUpdateCommentRequest(
        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = Comment.CONTENT_MAX_LENGTH, message = "내용은 최대 500자까지 가능합니다.")
        @Schema(example = "수정된 댓글 내용") String content
) {
}
