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

    @Operation(summary = "대목 생성", description = "도서에 새로운 대목을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대목 등록 성공"),
            @ApiResponse(
                    responseCode = "404", 
                    description = "BOOK_404_1: 해당 도서를 찾을 수 없습니다.<br>USER_404_1: 해당 사용자를 찾을 수 없습니다.", 
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"type\":\"/api/v1/passages\",\"title\":\"BOOK_404_1\",\"status\":404,\"detail\":\"해당 도서를 찾을 수 없습니다.\"}"))
            )
    })
    DataResponse<PassageResponse.Detail> createPassage(
            @Valid @RequestBody PassageRequest.Create request
    );
}
