package com.nexters.palang.domain.book.presentation;

import com.nexters.palang.domain.book.presentation.dto.BookActivityListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookResponse;
import com.nexters.palang.domain.book.presentation.dto.CreateBookRequest;
import com.nexters.palang.domain.book.presentation.dto.ExternalBookListResponse;
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

@Tag(name = "Book", description = "도서 API")
public interface BookApi {

    @Operation(summary = "도서 외부 검색",
            description = "알라딘 Open API로 도서를 검색합니다. 응답 속도를 위해 pageCount는 채우지 않으며 항상 null입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "keyword 누락, page/size 형식 오류(COMMON_400_1) "
                    + "또는 알라딘 검색 실패(BOOK_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<ExternalBookListResponse>> searchExternalBooks(
            @Parameter(description = "검색 키워드", required = true) String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "도서 내부 검색", description = "서비스 DB에 이미 등록된 도서를 제목으로 검색합니다. (FR-HOME-03)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "keyword 누락 또는 page/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<BookListResponse>> searchInternalBooks(
            @Parameter(description = "검색 키워드", required = true) String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "도서 직접 등록", description = "검색 결과에 없는 도서를 직접 등록합니다. 인증 불필요. (FR-WRITE-03)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락 또는 페이지수가 1 미만 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<BookResponse>> createBook(
            @Valid CreateBookRequest request
    );

    @Operation(summary = "홈 캐러셀 도서 목록", description = "흔적이 남은 도서를 대목/흔적 수와 함께 조회합니다. (FR-HOME-01,02)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "page/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<BookActivityListResponse>> getHomeCarouselBooks(
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "내가 최근에 남긴 도서 목록",
            description = "현재 로그인한 사용자가 최근에 대목을 남긴 도서 목록입니다. "
                    + "X-Debug-User-Id 헤더로 인증합니다(임시 스탠드인). (FR-WRITE-01)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "page/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-Debug-User-Id 헤더 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<BookListResponse>> getRecentBooks(
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "흔적 많은 도서 목록", description = "서비스 전체에서 흔적(Opinion)이 많은 순으로 도서를 조회합니다. (FR-WRITE-01)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "page/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<BookActivityListResponse>> getPopularBooks(
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );
}
