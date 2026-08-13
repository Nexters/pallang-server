package com.nexters.palang.domain.comment.application;

import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.comment.presentation.dto.CommentListResponse;
import com.nexters.palang.domain.comment.presentation.dto.CommentResponse;
import com.nexters.palang.domain.comment.presentation.dto.RootCommentListResponse;
import com.nexters.palang.domain.comment.presentation.dto.RootCommentResponse;
import com.nexters.palang.global.common.response.PageInfo;
import org.springframework.data.domain.Page;

public final class CommentMapper {

    private static final String DELETED_CONTENT_PLACEHOLDER = "삭제된 댓글입니다.";

    private CommentMapper() {
    }

    public static CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                comment.getUser().getProfileImageUrl(),
                comment.isDeleted() ? DELETED_CONTENT_PLACEHOLDER : comment.getContent(),
                comment.isDeleted(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }

    public static RootCommentResponse toRootResponse(RootCommentGroup group) {
        Comment comment = group.comment();
        return new RootCommentResponse(
                comment.getId(),
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                comment.getUser().getProfileImageUrl(),
                comment.isDeleted() ? DELETED_CONTENT_PLACEHOLDER : comment.getContent(),
                comment.isDeleted(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                group.replyPreview().stream().map(CommentMapper::toResponse).toList(),
                (int) group.replyCount(),
                group.hasMoreReplies());
    }

    public static RootCommentListResponse toRootListResponse(Page<RootCommentGroup> groups) {
        return new RootCommentListResponse(
                groups.map(CommentMapper::toRootResponse).getContent(), PageInfo.from(groups));
    }

    public static CommentListResponse toListResponse(Page<Comment> comments) {
        return new CommentListResponse(comments.map(CommentMapper::toResponse).getContent(), PageInfo.from(comments));
    }
}
