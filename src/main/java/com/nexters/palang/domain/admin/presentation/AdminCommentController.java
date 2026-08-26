package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.application.AdminAccessGuard;
import com.nexters.palang.domain.admin.application.AdminCommentMapper;
import com.nexters.palang.domain.admin.application.AdminCommentService;
import com.nexters.palang.domain.admin.presentation.dto.AdminCommentListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminCommentSummaryResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminUpdateCommentRequest;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.global.common.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminCommentController implements AdminCommentApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AdminAccessGuard adminAccessGuard;
    private final AdminCommentService adminCommentService;

    @Override
    @GetMapping("/api/admin/comments")
    public ResponseEntity<DataResponse<AdminCommentListResponse>> searchComments(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        adminAccessGuard.requireAdmin();
        Page<Comment> results = adminCommentService.searchComments(keyword, pageable(page, size));
        return ResponseEntity.ok(DataResponse.from(AdminCommentMapper.toListResponse(results)));
    }

    @Override
    @PatchMapping("/api/admin/comments/{commentId}")
    public ResponseEntity<DataResponse<AdminCommentSummaryResponse>> updateComment(
            @PathVariable Long commentId, @Valid @RequestBody AdminUpdateCommentRequest request) {
        adminAccessGuard.requireAdmin();
        Comment comment = adminCommentService.updateComment(commentId, request.content());
        return ResponseEntity.ok(DataResponse.from(AdminCommentMapper.toSummary(comment)));
    }

    @Override
    @DeleteMapping("/api/admin/comments/{commentId}")
    public ResponseEntity<DataResponse<Void>> deleteComment(@PathVariable Long commentId) {
        adminAccessGuard.requireAdmin();
        adminCommentService.deleteComment(commentId);
        return ResponseEntity.ok(DataResponse.from(null));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
