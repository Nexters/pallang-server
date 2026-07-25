package com.nexters.palang.domain.opinion.presentation;

import com.nexters.palang.domain.opinion.presentation.dto.CreateOpinionRequest;
import com.nexters.palang.domain.opinion.presentation.dto.OpinionResponse;
import com.nexters.palang.global.common.error.ErrorResponse;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-Debug-User-Id 헤더 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "해당 도서를 찾을 수 없음(BOOK_404_1) 또는 해당 대목을 찾을 수 없음(PASSAGE_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<OpinionResponse>> createOpinion(
            @Valid CreateOpinionRequest request
    );
}
