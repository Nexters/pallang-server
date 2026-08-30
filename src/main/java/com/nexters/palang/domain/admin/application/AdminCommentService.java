package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.comment.common.CommentErrorCode;
import com.nexters.palang.domain.comment.common.CommentException;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.comment.infrastructure.CommentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 페이지에서 댓글(Comment, 답글 포함)을 조회/수정/삭제하기 위한 서비스(issue #155 후속).
// CommentService의 modifyComment/removeComment는 작성자 본인만 호출할 수 있어(validateOwner) 관리자
// 용도로 재사용할 수 없다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCommentService {

    private final CommentRepository commentRepository;

    public Page<Comment> searchComments(String keyword, Pageable pageable) {
        return commentRepository.findByContentContaining(keyword, pageable);
    }

    @Transactional
    public Comment updateComment(Long commentId, String content) {
        Comment comment = getExistingComment(commentId);
        comment.updateContent(content);
        return comment;
    }

    // 원댓글이면 거기 달린 답글부터 지운다(자기참조 FK). 하드 삭제라 되돌릴 수 없다.
    @Transactional
    public void deleteComment(Long commentId) {
        getExistingComment(commentId);
        commentRepository.deleteAllByParentCommentIdIn(List.of(commentId));
        commentRepository.deleteById(commentId);
    }

    private Comment getExistingComment(Long commentId) {
        return commentRepository.findWithAssociationsById(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));
    }
}
