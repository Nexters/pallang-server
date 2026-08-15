package com.nexters.palang.domain.book.presentation;

import com.nexters.palang.domain.book.application.BookSearchSort;
import com.nexters.palang.domain.book.presentation.dto.BookActivityListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookCarouselListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookResponse;
import com.nexters.palang.domain.book.presentation.dto.BookSearchListResponse;
import com.nexters.palang.domain.book.presentation.dto.CreateBookRequest;
import com.nexters.palang.domain.book.presentation.dto.ExternalBookListResponse;
import com.nexters.palang.global.common.error.ErrorResponse;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Book", description = "도서 API")
public interface BookApi {

    @Operation(summary = "도서 외부 검색",
            description = "알라딘 Open API로 도서를 검색합니다. 응답 속도를 위해 pageCount는 내려주지 않으며, "
                    + "등록 시 사용자가 직접 입력합니다. 타이핑 중 자동완성 미리보기 용도로도 재사용되므로, "
                    + "keyword가 공백 제거 후 2글자 미만이면 알라딘을 호출하지 않고 빈 목록을 반환합니다. "
                    + "동일 keyword+size 조합은 최대 12시간 캐싱되어 결과가 즉시 반영되지 않을 수 있습니다.")
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

    @Operation(summary = "도서 내부 검색",
            description = "서비스 DB에 등록된 도서 전체를 흔적(Opinion) 유무와 무관하게 제목으로 검색하며, "
                    + "도서별 대목/흔적 수를 함께 반환합니다(흔적이 없으면 0). "
                    + "제목과 검색어의 띄어쓰기 차이는 무시하고 매칭합니다. keyword에 빈 문자열을 넘기면 전체 목록을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "keyword 누락, sort 값 오류 또는 page/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<BookSearchListResponse>> searchInternalBooks(
            @Parameter(description = "검색 키워드 (빈 문자열이면 전체 목록)", required = true) String keyword,
            @Parameter(description = "정렬 기준 (NAME: 이름순, RECENT: 최신 흔적순, OPINION: 흔적 많은 순, 기본값 RECENT)")
            BookSearchSort sort,
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "도서 직접 등록", description = "검색 결과에 없는 도서를 직접 등록합니다. 인증 불필요. "
            + "multipart/form-data로 요청하며, coverImage는 선택 항목입니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    // book 파트는 JSON 객체다. springdoc이 encoding을 자동으로 채워주지 않아
                    // 명시하지 않으면 Swagger UI가 application/octet-stream으로 보내 400/415가 난다.
                    encoding = @Encoding(name = "book", contentType = MediaType.APPLICATION_JSON_VALUE)
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락, 페이지수가 1 미만 (COMMON_400_1) "
                    + "또는 이미지 파일이 아님 (BOOK_400_3)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<BookResponse>> createBook(
            @Valid CreateBookRequest request,
            @Parameter(description = "책 표지 이미지 파일 (jpeg/png, 선택)") MultipartFile coverImage
    );

    @Operation(summary = "홈 캐러셀 도서 목록",
            description = "흔적이 남은 도서를 대목/흔적 수와 함께 조회합니다. offset을 생략하면 전체 목록 중 "
                    + "정가운데 책들을 기준으로 조회하며, 좌우 스크롤 시에는 응답으로 받은 pageInfo를 참고해 "
                    + "offset - size(이전) 또는 offset + size(다음)로 다시 요청하면 됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "offset/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<BookCarouselListResponse>> getHomeCarouselBooks(
            @Parameter(description = "조회 시작 위치 (0부터 시작). 생략하면 전체 목록 중 가운데를 기준으로 조회") Long offset,
            @Parameter(description = "조회 개수 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "내 서재 도서 목록",
            description = "현재 로그인한 사용자가 흔적을 남긴 도서만 대상으로, 대목/흔적 수와 함께 조회합니다. "
                    + "가장 최근에 흔적을 남긴 도서부터 내림차순으로 정렬되며, offset을 생략하면 0부터(=가장 최근 "
                    + "도서부터) 조회합니다. 더 과거 도서를 이어서 보려면 응답으로 받은 pageInfo를 참고해 "
                    + "offset + size로 다시 요청하면 됩니다. Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "offset/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<BookCarouselListResponse>> getMyLibraryBooks(
            @Parameter(description = "조회 시작 위치 (0부터 시작). 생략하면 가장 최근에 흔적을 남긴 도서부터 조회") Long offset,
            @Parameter(description = "조회 개수 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "내가 최근에 남긴 도서 목록",
            description = "현재 로그인한 사용자가 최근에 대목을 남긴 도서 목록입니다. "
                    + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "page/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<BookListResponse>> getRecentBooks(
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "흔적 많은 도서 목록", description = "서비스 전체에서 흔적(Opinion)이 많은 순으로 도서를 조회합니다.")
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
