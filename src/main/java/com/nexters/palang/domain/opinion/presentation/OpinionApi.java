package com.nexters.palang.domain.opinion.presentation;

import com.nexters.palang.domain.opinion.domain.OpinionSortType;
import com.nexters.palang.domain.opinion.presentation.dto.CreateOpinionRequest;
import com.nexters.palang.domain.opinion.presentation.dto.OpinionDetailResponse;
import com.nexters.palang.domain.opinion.presentation.dto.OpinionListResponse;
import com.nexters.palang.domain.opinion.presentation.dto.OpinionResponse;
import com.nexters.palang.domain.opinion.presentation.dto.UpdateOpinionRequest;
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

@Tag(name = "Opinion", description = "흔적(Opinion) API")
public interface OpinionApi {

    @Operation(summary = "흔적 작성 (직접 입력)",
            description = "Passage(신규 생성 또는 기존 병합) + Opinion + Decoration을 원자적으로 생성합니다. "
                    + "passageId가 없으면 새 Passage를 만들고, 있으면 해당 Passage에 병합합니다(Q-06). "
                    + "OCR 입력은 별도 플로우이며 이 API는 직접 입력만 지원합니다. "
                    + "X-Debug-User-Id 헤더로 인증합니다(임시 스탠드인). (FR-WRITE-06~10)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "흔적 생성 성공"),
            @ApiResponse(responseCode = "400",
                    description = "필수값 누락, 인용 문구/내용 길이 초과(COMMON_400_1), "
                            + "선택한 대목이 지정한 도서와 불일치(PASSAGE_400_2), "
                            + "꾸밈 효과 범위 오류(DECORATION_400_1) 또는 겹침(DECORATION_400_2)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "COMMON_400_1: 필수값 누락/길이 초과", value = "{\"type\":\"/api/opinions\",\"title\":\"COMMON_400_1\",\"status\":400,\"detail\":\"인용 문구는 150자를 초과할 수 없습니다.\"}"),
                                    @ExampleObject(name = "PASSAGE_400_2: 도서 불일치", value = "{\"type\":\"/api/opinions\",\"title\":\"PASSAGE_400_2\",\"status\":400,\"detail\":\"선택한 대목이 지정한 도서와 일치하지 않습니다.\"}"),
                                    @ExampleObject(name = "DECORATION_400_1: 꾸밈 범위 오류", value = "{\"type\":\"/api/opinions\",\"title\":\"DECORATION_400_1\",\"status\":400,\"detail\":\"꾸밈 효과의 시작 위치는 끝 위치보다 작아야 합니다.\"}"),
                                    @ExampleObject(name = "DECORATION_400_2: 꾸밈 영역 겹침", value = "{\"type\":\"/api/opinions\",\"title\":\"DECORATION_400_2\",\"status\":400,\"detail\":\"같은 흔적 안에서는 꾸밈 효과 영역이 겹칠 수 없습니다.\"}")
                            })
            ),
            @ApiResponse(responseCode = "401", description = "X-Debug-User-Id 헤더 누락 (AUTH_401_1)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"type\":\"/api/opinions\",\"title\":\"AUTH_401_1\",\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))
            ),
            @ApiResponse(responseCode = "404",
                    description = "해당 도서를 찾을 수 없음(BOOK_404_1) 또는 해당 대목을 찾을 수 없음(PASSAGE_404_1)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "BOOK_404_1: 도서 없음", value = "{\"type\":\"/api/opinions\",\"title\":\"BOOK_404_1\",\"status\":404,\"detail\":\"해당 도서를 찾을 수 없습니다.\"}"),
                                    @ExampleObject(name = "PASSAGE_404_1: 대목 없음", value = "{\"type\":\"/api/opinions\",\"title\":\"PASSAGE_404_1\",\"status\":404,\"detail\":\"해당 대목을 찾을 수 없습니다.\"}")
                            })
            )
    })
    ResponseEntity<DataResponse<OpinionResponse>> createOpinion(
            @Valid CreateOpinionRequest request
    );

    @Operation(summary = "흔적 목록 조회", description = "특정 대목에 남겨진 흔적을 정렬 기준(최신순 기본/좋아요순)으로 조회합니다. (FR-OPINION-03)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "page/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/passages/1/opinions\",\"title\":\"COMMON_400_1\",\"status\":400,\"detail\":\"'size' 파라미터의 값이 올바르지 않습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 대목을 찾을 수 없음 (PASSAGE_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/passages/1/opinions\",\"title\":\"PASSAGE_404_1\",\"status\":404,\"detail\":\"해당 대목을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<OpinionListResponse>> getOpinions(
            @Parameter(description = "대목 ID", required = true) Long passageId,
            @Parameter(description = "정렬 기준 (LATEST: 최신순(기본), LIKES: 좋아요순)") OpinionSortType sortType,
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "흔적 상세 조회", description = "흔적 작성자가 기록한 꾸밈을 그대로 확인합니다(병합된 결과가 아님). (FR-OPINION-05)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 흔적을 찾을 수 없음 (OPINION_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/opinions/1\",\"title\":\"OPINION_404_1\",\"status\":404,\"detail\":\"해당 흔적을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<OpinionDetailResponse>> getOpinion(
            @Parameter(description = "흔적 ID", required = true) Long opinionId
    );

    @Operation(summary = "흔적 수정", description = "본인이 작성한 흔적의 내용을 수정합니다. 꾸밈은 생성 시점에 고정되며 수정 대상이 아닙니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "내용 누락/길이 초과 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/opinions/1\",\"title\":\"COMMON_400_1\",\"status\":400,\"detail\":\"흔적 내용은 500자를 초과할 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "X-Debug-User-Id 헤더 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/opinions/1\",\"title\":\"AUTH_401_1\",\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "OPINION_403_1: 본인이 작성한 흔적만 수정/삭제할 수 있습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/opinions/1\",\"title\":\"OPINION_403_1\",\"status\":403,\"detail\":\"본인이 작성한 흔적만 수정/삭제할 수 있습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 흔적을 찾을 수 없음 (OPINION_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/opinions/1\",\"title\":\"OPINION_404_1\",\"status\":404,\"detail\":\"해당 흔적을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<OpinionDetailResponse>> modifyOpinion(
            @Parameter(description = "흔적 ID", required = true) Long opinionId,
            @Valid UpdateOpinionRequest request
    );

    @Operation(summary = "흔적 삭제", description = "본인이 작성한 흔적을 소프트 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "X-Debug-User-Id 헤더 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/opinions/1\",\"title\":\"AUTH_401_1\",\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "OPINION_403_1: 본인이 작성한 흔적만 수정/삭제할 수 있습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/opinions/1\",\"title\":\"OPINION_403_1\",\"status\":403,\"detail\":\"본인이 작성한 흔적만 수정/삭제할 수 있습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 흔적을 찾을 수 없음 (OPINION_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/opinions/1\",\"title\":\"OPINION_404_1\",\"status\":404,\"detail\":\"해당 흔적을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<Void>> removeOpinion(
            @Parameter(description = "흔적 ID", required = true) Long opinionId
    );
}
