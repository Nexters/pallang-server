package com.nexters.palang.domain.comment.presentation;

import com.nexters.palang.domain.comment.application.CommentMapper;
import com.nexters.palang.domain.comment.application.CommentService;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.comment.presentation.dto.CommentListResponse;
import com.nexters.palang.domain.comment.presentation.dto.CommentResponse;
import com.nexters.palang.domain.comment.presentation.dto.CreateCommentRequest;
import com.nexters.palang.domain.comment.presentation.dto.RootCommentListResponse;
import com.nexters.palang.domain.comment.presentation.dto.UpdateCommentRequest;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentController implements CommentApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final CommentService commentService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @GetMapping("/api/opinions/{opinionId}/comments")
    public ResponseEntity<DataResponse<RootCommentListResponse>> getComments(
            @PathVariable Long opinionId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(DataResponse.from(
                CommentMapper.toRootListResponse(commentService.getRootComments(opinionId, pageable(page, size)))));
    }

    @Override
    @GetMapping("/api/comments/{commentId}/replies")
    public ResponseEntity<DataResponse<CommentListResponse>> getReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(DataResponse.from(
                CommentMapper.toListResponse(commentService.getReplies(commentId, pageable(page, size)))));
    }

    @Override
    @PostMapping("/api/opinions/{opinionId}/comments")
    public ResponseEntity<DataResponse<CommentResponse>> createComment(
            @PathVariable Long opinionId,
            @RequestBody @Valid CreateCommentRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Comment comment = commentService.createComment(opinionId, currentUserId, request);
        return ResponseEntity.ok(DataResponse.from(CommentMapper.toResponse(comment)));
    }

    @Override
    @PatchMapping("/api/comments/{commentId}")
    public ResponseEntity<DataResponse<CommentResponse>> modifyComment(
            @PathVariable Long commentId,
            @RequestBody @Valid UpdateCommentRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Comment comment = commentService.modifyComment(commentId, currentUserId, request);
        return ResponseEntity.ok(DataResponse.from(CommentMapper.toResponse(comment)));
    }

    @Override
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<DataResponse<Void>> removeComment(@PathVariable Long commentId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        commentService.removeComment(commentId, currentUserId);
        return ResponseEntity.ok(DataResponse.from(null));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
