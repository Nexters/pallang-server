package com.nexters.palang.domain.book.presentation;

import com.nexters.palang.domain.book.presentation.dto.UpdateUserBookStatusRequest;
import com.nexters.palang.domain.book.presentation.dto.UserBookStatusResponse;
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

@Tag(name = "UserBookStatus", description = "유저별 도서 읽기상태 API")
public interface UserBookStatusApi {

    @Operation(summary = "읽기상태/현재페이지 설정", description = "로그인한 사용자의 특정 도서에 대한 읽기상태와 현재 페이지를 설정합니다. "
            + "기존 상태가 없으면 새로 생성합니다. X-Debug-User-Id 헤더로 인증합니다(임시 스탠드인). (FR-WRITE-08)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설정 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락 또는 현재 페이지가 전체 페이지 수 초과(BOOK_400_2)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-Debug-User-Id 헤더 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 도서를 찾을 수 없음 (BOOK_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<UserBookStatusResponse>> updateBookStatus(
            @Valid UpdateUserBookStatusRequest request
    );
}
