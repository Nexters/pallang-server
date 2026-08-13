package com.nexters.palang.domain.report.presentation;

import com.nexters.palang.domain.report.presentation.dto.CreateReportRequest;
import com.nexters.palang.domain.report.presentation.dto.ReportResponse;
import com.nexters.palang.global.common.error.ErrorResponse;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Report", description = "신고 API")
public interface ReportApi {

    @Operation(summary = "흔적 신고", description = "흔적을 사유(스팸/혐오/욕설/저작권/기타)와 함께 신고합니다. "
            + "기타 사유는 상세 내용이 필수입니다. 본인이 작성한 흔적, 이미 신고한 흔적은 신고할 수 없습니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고 접수 성공"),
            @ApiResponse(responseCode = "400", description = "사유 누락(COMMON_400_1) 또는 기타 사유에 상세 내용 누락(REPORT_400_1) "
                    + "또는 본인 흔적 신고 시도(REPORT_400_2)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "REPORT_400_1", value = "{\"type\":\"/api/opinions/1/reports\","
                                    + "\"title\":\"REPORT_400_1\",\"status\":400,"
                                    + "\"detail\":\"기타 사유를 선택한 경우 상세 내용을 입력해야 합니다.\"}"),
                            @ExampleObject(name = "REPORT_400_2", value = "{\"type\":\"/api/opinions/1/reports\","
                                    + "\"title\":\"REPORT_400_2\",\"status\":400,"
                                    + "\"detail\":\"본인이 작성한 콘텐츠는 신고할 수 없습니다.\"}")
                    })),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/opinions/1/reports\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 흔적을 찾을 수 없음 (OPINION_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/opinions/1/reports\",\"title\":\"OPINION_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 흔적을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 신고한 대상 (REPORT_409_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/opinions/1/reports\",\"title\":\"REPORT_409_1\","
                                    + "\"status\":409,\"detail\":\"이미 신고한 대상입니다.\"}")))
    })
    ResponseEntity<DataResponse<ReportResponse>> reportOpinion(
            @Parameter(description = "흔적 ID", required = true) Long opinionId,
            @Valid CreateReportRequest request
    );

    @Operation(summary = "댓글 신고", description = "댓글/답글을 사유(스팸/혐오/욕설/저작권/기타)와 함께 신고합니다. "
            + "기타 사유는 상세 내용이 필수입니다. 본인이 작성한 댓글, 이미 신고한 댓글은 신고할 수 없습니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고 접수 성공"),
            @ApiResponse(responseCode = "400", description = "사유 누락(COMMON_400_1) 또는 기타 사유에 상세 내용 누락(REPORT_400_1) "
                    + "또는 본인 댓글 신고 시도(REPORT_400_2)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "REPORT_400_1", value = "{\"type\":\"/api/comments/1/reports\","
                                    + "\"title\":\"REPORT_400_1\",\"status\":400,"
                                    + "\"detail\":\"기타 사유를 선택한 경우 상세 내용을 입력해야 합니다.\"}"),
                            @ExampleObject(name = "REPORT_400_2", value = "{\"type\":\"/api/comments/1/reports\","
                                    + "\"title\":\"REPORT_400_2\",\"status\":400,"
                                    + "\"detail\":\"본인이 작성한 콘텐츠는 신고할 수 없습니다.\"}")
                    })),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/comments/1/reports\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 댓글을 찾을 수 없음 (COMMENT_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/comments/1/reports\",\"title\":\"COMMENT_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 댓글을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 신고한 대상 (REPORT_409_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/comments/1/reports\",\"title\":\"REPORT_409_1\","
                                    + "\"status\":409,\"detail\":\"이미 신고한 대상입니다.\"}")))
    })
    ResponseEntity<DataResponse<ReportResponse>> reportComment(
            @Parameter(description = "댓글 ID", required = true) Long commentId,
            @Valid CreateReportRequest request
    );
}
