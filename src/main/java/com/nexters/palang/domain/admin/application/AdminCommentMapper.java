package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.admin.presentation.dto.AdminCommentListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminCommentSummaryResponse;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.global.common.response.PageInfo;
import org.springframework.data.domain.Page;

public final class AdminCommentMapper {

    private AdminCommentMapper() {
    }

    // opinion/user/parentComment는 지연 로딩 연관관계다. AdminPassageMapper와 같은 이유로 open-in-view에 기대고 있다.
    public static AdminCommentSummaryResponse toSummary(Comment comment) {
        return new AdminCommentSummaryResponse(
                comment.getId(), comment.getOpinion().getId(),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                comment.getUser().getId(), comment.getUser().getNickname(),
                comment.getContent(), comment.isDeleted(), comment.getCreatedAt());
    }

    public static AdminCommentListResponse toListResponse(Page<Comment> comments) {
        return new AdminCommentListResponse(comments.map(AdminCommentMapper::toSummary).getContent(), PageInfo.from(comments));
    }
}
