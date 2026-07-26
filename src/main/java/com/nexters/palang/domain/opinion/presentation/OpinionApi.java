package com.nexters.palang.domain.opinion.presentation;

import com.nexters.palang.domain.opinion.presentation.dto.CreateOpinionRequest;
import com.nexters.palang.domain.opinion.presentation.dto.OpinionLikeResponse;
import com.nexters.palang.domain.opinion.presentation.dto.OpinionResponse;
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

    @Operation(summary = "흔적 좋아요 토글",
            description = "좋아요를 누르지 않은 상태면 좋아요를 남기고, 이미 눌렀다면 취소합니다. "
                    + "likeCount는 opinion_likes 테이블에 걸린 DB 트리거로 동기화됩니다. "
                    + "X-Debug-User-Id 헤더로 인증합니다(임시 스탠드인).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좋아요 토글 성공"),
            @ApiResponse(responseCode = "401", description = "X-Debug-User-Id 헤더 누락 (AUTH_401_1)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"type\":\"/api/opinions/1/like\",\"title\":\"AUTH_401_1\",\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))
            ),
            @ApiResponse(responseCode = "404", description = "해당 흔적을 찾을 수 없음 (OPINION_404_1)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"type\":\"/api/opinions/1/like\",\"title\":\"OPINION_404_1\",\"status\":404,\"detail\":\"해당 흔적을 찾을 수 없습니다.\"}"))
            )
    })
    ResponseEntity<DataResponse<OpinionLikeResponse>> toggleOpinionLike(
            @Parameter(description = "흔적 ID", required = true) Long opinionId
    );
}
