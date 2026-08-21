package com.nexters.palang.domain.comment.application;

import com.nexters.palang.domain.comment.common.CommentErrorCode;
import com.nexters.palang.domain.comment.common.CommentException;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.comment.domain.event.CommentCreatedEvent;
import com.nexters.palang.domain.comment.infrastructure.CommentQueryRepository;
import com.nexters.palang.domain.comment.infrastructure.CommentRepository;
import com.nexters.palang.domain.comment.presentation.dto.CreateCommentRequest;
import com.nexters.palang.domain.comment.presentation.dto.UpdateCommentRequest;
import com.nexters.palang.domain.group.application.GroupAccessValidator;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.opinion.common.error.OpinionErrorCode;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.user.common.error.UserErrorCode;
import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    // FR-OPINION-07: 원댓글 아래 답글은 5개까지 미리보기로 노출하고, 그 이상은 "답글 더보기"로 별도 조회한다.
    private static final int REPLY_PREVIEW_SIZE = 5;

    private final CommentRepository commentRepository;
    private final CommentQueryRepository commentQueryRepository;
    private final OpinionRepository opinionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final GroupAccessValidator groupAccessValidator;

    // 흔적이 모임 전용이면(passage.group != null) 모임원만 댓글을 보거나 남길 수 있다.
    public Page<RootCommentGroup> getRootComments(Long opinionId, Pageable pageable, Long currentUserId) {
        Opinion opinion = getExistingOpinion(opinionId);
        validateGroupAccess(opinion, currentUserId);

        Page<Comment> roots = commentQueryRepository.findRootComments(opinionId, pageable, currentUserId);
        List<Long> rootIds = roots.getContent().stream().map(Comment::getId).toList();
        Map<Long, Long> replyCounts = commentQueryRepository.countRepliesByParentIds(rootIds, currentUserId);
        Map<Long, List<Comment>> replyPreviews =
                commentQueryRepository.findReplyPreviewsByParentIds(rootIds, REPLY_PREVIEW_SIZE, currentUserId);

        return roots.map(root -> new RootCommentGroup(
                root,
                replyPreviews.getOrDefault(root.getId(), List.of()),
                replyCounts.getOrDefault(root.getId(), 0L)));
    }

    public Page<Comment> getReplies(Long parentCommentId, Pageable pageable, Long currentUserId) {
        Comment parent = getExistingComment(parentCommentId);
        Opinion opinion = getExistingOpinion(parent.getOpinion().getId());
        validateGroupAccess(opinion, currentUserId);
        return commentQueryRepository.findReplies(parentCommentId, pageable, currentUserId);
    }

    @Transactional
    public Comment createComment(Long opinionId, Long userId, CreateCommentRequest request) {
        Opinion opinion = getExistingOpinion(opinionId);
        validateGroupAccess(opinion, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        Comment saved;
        if (request.parentCommentId() == null) {
            saved = commentRepository.save(Comment.root(opinion, user, request.content()));
        } else {
            Comment parent = getEditableComment(request.parentCommentId());
            if (!parent.getOpinion().getId().equals(opinionId)) {
                throw new CommentException(CommentErrorCode.COMMENT_NOT_FOUND);
            }
            saved = commentRepository.save(Comment.reply(parent, user, request.content()));
        }

        eventPublisher.publishEvent(
                new CommentCreatedEvent(saved.getId(), opinionId, opinion.getUser().getId(), userId));
        return saved;
    }

    @Transactional
    public Comment modifyComment(Long commentId, Long userId, UpdateCommentRequest request) {
        Comment comment = getEditableComment(commentId);
        validateOwner(comment, userId);
        comment.updateContent(request.content());
        return comment;
    }

    @Transactional
    public void removeComment(Long commentId, Long userId) {
        Comment comment = getEditableComment(commentId);
        validateOwner(comment, userId);
        comment.delete();
    }

    private void validateGroupAccess(Opinion opinion, Long userId) {
        Group group = opinion.getPassage().getGroup();
        if (group != null) {
            groupAccessValidator.validateMember(group.getId(), userId);
        }
    }

    private Opinion getExistingOpinion(Long opinionId) {
        Opinion opinion = opinionRepository.findById(opinionId)
                .orElseThrow(() -> new OpinionException(OpinionErrorCode.OPINION_NOT_FOUND));
        if (opinion.isDeleted()) {
            throw new OpinionException(OpinionErrorCode.OPINION_NOT_FOUND);
        }
        return opinion;
    }

    private Comment getExistingComment(Long commentId) {
        return commentRepository.findByIdWithUser(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    // 삭제된 댓글은 자리(placeholder)만 유지할 뿐 더 이상 수정/삭제 대상이 아니므로 "찾을 수 없음"으로 취급한다.
    private Comment getEditableComment(Long commentId) {
        Comment comment = getExistingComment(commentId);
        if (comment.isDeleted()) {
            throw new CommentException(CommentErrorCode.COMMENT_NOT_FOUND);
        }
        return comment;
    }

    private void validateOwner(Comment comment, Long userId) {
        if (!comment.getUser().getId().equals(userId)) {
            throw new CommentException(CommentErrorCode.COMMENT_FORBIDDEN);
        }
    }
}
