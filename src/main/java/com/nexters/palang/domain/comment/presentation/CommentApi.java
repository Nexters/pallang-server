package com.nexters.palang.domain.comment.presentation;

import com.nexters.palang.domain.comment.presentation.dto.CommentListResponse;
import com.nexters.palang.domain.comment.presentation.dto.CommentResponse;
import com.nexters.palang.domain.comment.presentation.dto.CreateCommentRequest;
import com.nexters.palang.domain.comment.presentation.dto.RootCommentListResponse;
import com.nexters.palang.domain.comment.presentation.dto.UpdateCommentRequest;
import com.nexters.palang.global.common.error.ErrorResponse;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Comment", description = "댓글 API")
public interface CommentApi {

    @Operation(summary = "댓글 목록 조회",
            description = "흔적에 달린 원댓글과 답글을 조회합니다. 원댓글은 page/size로 페이지네이션되며, "
                    + "각 원댓글에는 답글이 최대 5개까지 미리보기로 포함됩니다. 5개를 초과하면 hasMoreReplies=true이며, "
                    + "나머지는 GET /api/comments/{commentId}/replies로 조회합니다. (FR-OPINION-07)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "page/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 흔적을 찾을 수 없음 (OPINION_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<RootCommentListResponse>> getComments(
            @Parameter(description = "흔적 ID", required = true) Long opinionId,
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "답글 더보기",
            description = "특정 원댓글에 달린 답글을 page/size로 페이지네이션 조회합니다. (FR-OPINION-07)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "page/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 댓글을 찾을 수 없음 (COMMENT_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<CommentListResponse>> getReplies(
            @Parameter(description = "원댓글 ID", required = true) Long commentId,
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "댓글/답글 작성",
            description = "흔적에 댓글 또는 답글을 작성합니다. parentCommentId가 없으면 원댓글, 있으면 답글로 생성됩니다. "
                    + "답글에는 다시 답글을 남길 수 없습니다(1-depth). 삭제된 댓글을 부모로 답글을 작성할 수 없습니다. "
                    + "X-Debug-User-Id 헤더로 인증합니다(임시 스탠드인).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "작성 성공"),
            @ApiResponse(responseCode = "400", description = "내용 누락/500자 초과 (COMMON_400_1) "
                    + "또는 답글에 답글 시도 (COMMENT_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-Debug-User-Id 헤더 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "흔적 또는 부모 댓글을 찾을 수 없음 "
                    + "(OPINION_404_1/COMMENT_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<CommentResponse>> createComment(
            @Parameter(description = "흔적 ID", required = true) Long opinionId,
            @Valid CreateCommentRequest request
    );

    @Operation(summary = "댓글/답글 수정", description = "본인이 작성한 댓글/답글의 내용을 수정합니다. "
            + "X-Debug-User-Id 헤더로 인증합니다(임시 스탠드인).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "내용 누락/500자 초과 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-Debug-User-Id 헤더 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인이 작성한 댓글이 아님 (COMMENT_403_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 댓글을 찾을 수 없음 (COMMENT_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<CommentResponse>> modifyComment(
            @Parameter(description = "댓글 ID", required = true) Long commentId,
            @Valid UpdateCommentRequest request
    );

    @Operation(summary = "댓글/답글 삭제", description = "본인이 작성한 댓글/답글을 소프트 삭제합니다. "
            + "X-Debug-User-Id 헤더로 인증합니다(임시 스탠드인).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "X-Debug-User-Id 헤더 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인이 작성한 댓글이 아님 (COMMENT_403_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 댓글을 찾을 수 없음 (COMMENT_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<Void>> removeComment(
            @Parameter(description = "댓글 ID", required = true) Long commentId
    );
}
