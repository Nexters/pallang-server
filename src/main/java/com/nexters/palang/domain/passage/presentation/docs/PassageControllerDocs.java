package com.nexters.palang.domain.passage.presentation.docs;

import com.nexters.palang.domain.passage.presentation.request.PassageRequest;
import com.nexters.palang.domain.passage.presentation.response.PassageResponse;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.nexters.palang.global.common.error.ErrorResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "대목(Passage)", description = "대목 및 OCR 관련 API")
public interface PassageControllerDocs {

    @Operation(summary = "이미지 OCR 텍스트 추출", description = "도서의 특정 페이지 이미지를 업로드하여 텍스트를 추출(인식)합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "텍스트 추출 성공"),
            @ApiResponse(
                    responseCode = "400", 
                    description = "PASSAGE_400_1: 이미지 파일만 업로드할 수 있습니다.", 
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"type\":\"/api/v1/passages/ocr\",\"title\":\"PASSAGE_400_1\",\"status\":400,\"detail\":\"이미지 파일만 업로드할 수 있습니다.\"}"))
            ),
            @ApiResponse(
                    responseCode = "422", 
                    description = "PASSAGE_422_1: 이미지에서 텍스트를 인식하지 못했습니다.", 
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"type\":\"/api/v1/passages/ocr\",\"title\":\"PASSAGE_422_1\",\"status\":422,\"detail\":\"이미지에서 텍스트를 인식하지 못했습니다.\"}"))
            ),
            @ApiResponse(
                    responseCode = "503", 
                    description = "PASSAGE_503_1: OCR 처리 중 오류가 발생했습니다.", 
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"type\":\"/api/v1/passages/ocr\",\"title\":\"PASSAGE_503_1\",\"status\":503,\"detail\":\"OCR 처리 중 오류가 발생했습니다.\"}"))
            )
    })
    DataResponse<PassageResponse.OcrRecognize> createOcrResult(
            @Parameter(description = "텍스트를 추출할 이미지 파일 (지원 포맷: JPEG, PNG)") 
            @RequestPart("image") MultipartFile image
    );

    @Operation(summary = "유사 문장 후보 조회", description = "저장 전 같은 도서의 인접 페이지(±1)에서 정규화 해시가 같은 대목 후보를 조회합니다. "
            + "X-Debug-User-Id 헤더로 인증합니다(임시 스탠드인). (FR-WRITE-07)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (후보가 없으면 빈 배열)"),
            @ApiResponse(
                    responseCode = "400",
                    description = "필수값 누락 또는 인용 문구 150자 초과 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"type\":\"/api/passages/similar-check\",\"title\":\"COMMON_400_1\",\"status\":400,\"detail\":\"인용 문구는 150자를 초과할 수 없습니다.\"}"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "AUTH_401_1: 로그인이 필요합니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"type\":\"/api/passages/similar-check\",\"title\":\"AUTH_401_1\",\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "BOOK_404_1: 해당 도서를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"type\":\"/api/passages/similar-check\",\"title\":\"BOOK_404_1\",\"status\":404,\"detail\":\"해당 도서를 찾을 수 없습니다.\"}"))
            )
    })
    DataResponse<PassageResponse.SimilarCandidates> checkSimilarPassages(
            @Valid @RequestBody PassageRequest.SimilarCheck request
    );

    @Operation(summary = "대목 페이지 목록 조회",
            description = "도서에서 발췌된 페이지 번호를 오름차순으로 조회합니다(FR-VIEW-02). "
                    + "읽기상태 기반 노출 필터가 적용됩니다: 읽는 중이면 현재 페이지까지, "
                    + "읽을 예정/미설정/비로그인이면 첫 페이지만 노출됩니다(FR-WRITE-08). "
                    + "스포일러 대목이 있는 페이지도 이 목록에 포함됩니다(스포일러 블러 처리는 프론트 담당, FR-VIEW-03). "
                    + "인증은 선택입니다(soft auth) — 헤더가 없어도 비로그인 기준으로 조회됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (대목이 없으면 빈 배열)"),
            @ApiResponse(responseCode = "400", description = "page/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/books/1/passages\",\"title\":\"COMMON_400_1\",\"status\":400,\"detail\":\"'size' 파라미터의 값이 올바르지 않습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "BOOK_404_1: 해당 도서를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/books/1/passages\",\"title\":\"BOOK_404_1\",\"status\":404,\"detail\":\"해당 도서를 찾을 수 없습니다.\"}")))
    })
    DataResponse<PassageResponse.PageNumbers> getPageNumbers(
            @Parameter(description = "도서 ID", required = true) Long bookId,
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "특정 페이지의 대목 + 꾸밈 병합 결과 조회",
            description = "같은 페이지에 여러 대목이 있으면 모두 반환합니다(대목 전환, FR-VIEW-03). "
                    + "각 대목에는 좋아요 많은 순 최대 3개, 겹치지 않는 꾸밈만 병합되어 포함됩니다. "
                    + "스포일러로 표기된 대목(isSpoiler=true)도 quotedText/decorations를 그대로 내려줍니다 — "
                    + "블러 처리 후 [버튼]을 누르면 즉시 확인 가능해야 하므로(FR-VIEW-03 스포일러 처리), "
                    + "블러/확인 전환은 서버 왕복 없이 프론트에서 isSpoiler 플래그로 처리합니다. "
                    + "읽기상태 노출 필터를 만족하지 않는 페이지를 요청하면 빈 배열이 반환되며, "
                    + "비로그인 사용자가 첫 페이지가 아닌 페이지를 요청하면 401로 로그인을 유도합니다(FR-OPINION-08).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (노출 범위 밖이면 빈 배열)"),
            @ApiResponse(responseCode = "401", description = "AUTH_401_1: 비로그인 사용자가 첫 페이지가 아닌 페이지를 요청함.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/books/1/pages/5/passages\",\"title\":\"AUTH_401_1\",\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "BOOK_404_1: 해당 도서를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/books/1/pages/5/passages\",\"title\":\"BOOK_404_1\",\"status\":404,\"detail\":\"해당 도서를 찾을 수 없습니다.\"}")))
    })
    DataResponse<PassageResponse.PassagesByPage> getPassagesByPage(
            @Parameter(description = "도서 ID", required = true) Long bookId,
            @Parameter(description = "페이지 번호", required = true) int page
    );
}
