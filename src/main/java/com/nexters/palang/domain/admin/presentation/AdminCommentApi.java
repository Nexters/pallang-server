package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.presentation.dto.AdminCommentListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminCommentSummaryResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminUpdateCommentRequest;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin", description = "관리자 전용 API — 테스트 계정 정리 등")
public interface AdminCommentApi {

    @Operation(summary = "관리자 댓글 검색", description = "내용에 키워드가 포함된 댓글(답글 포함)을 검색합니다. "
            + "소프트 삭제된 댓글도 함께 조회됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)")
    })
    ResponseEntity<DataResponse<AdminCommentListResponse>> searchComments(
            @Parameter(description = "내용 검색어") String keyword,
            @Parameter(hidden = true) int page,
            @Parameter(hidden = true) int size
    );

    @Operation(summary = "관리자 댓글 수정", description = "내용을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)"),
            @ApiResponse(responseCode = "404", description = "해당 댓글을 찾을 수 없음 (COMMENT_404_1)")
    })
    ResponseEntity<DataResponse<AdminCommentSummaryResponse>> updateComment(
            Long commentId, AdminUpdateCommentRequest request);

    @Operation(summary = "관리자 댓글 삭제", description = "댓글을 하드 삭제합니다. 원댓글이면 거기 달린 "
            + "답글도 함께 삭제됩니다. 되돌릴 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)"),
            @ApiResponse(responseCode = "404", description = "해당 댓글을 찾을 수 없음 (COMMENT_404_1)")
    })
    ResponseEntity<DataResponse<Void>> deleteComment(Long commentId);
}
