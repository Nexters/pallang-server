package com.nexters.palang.domain.book.presentation;

import com.nexters.palang.domain.book.presentation.dto.BookActivityListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookResponse;
import com.nexters.palang.domain.book.presentation.dto.CreateBookRequest;
import com.nexters.palang.domain.book.presentation.dto.ExternalBookListResponse;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Book", description = "도서 API")
public interface BookApi {

    @Operation(summary = "도서 외부 검색", description = "알라딘 Open API로 도서를 검색합니다.")
    ResponseEntity<DataResponse<ExternalBookListResponse>> searchExternalBooks(
            @Parameter(description = "검색 키워드") String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)") int page,
            @Parameter(description = "페이지 크기") int size
    );

    @Operation(summary = "도서 내부 검색", description = "서비스 DB에 이미 등록된 도서를 제목으로 검색합니다. (FR-HOME-03)")
    ResponseEntity<DataResponse<BookListResponse>> searchInternalBooks(
            @Parameter(description = "검색 키워드") String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)") int page,
            @Parameter(description = "페이지 크기") int size
    );

    @Operation(summary = "도서 직접 등록", description = "검색 결과에 없는 도서를 직접 등록합니다. (FR-WRITE-03)")
    ResponseEntity<DataResponse<BookResponse>> createBook(
            @Valid CreateBookRequest request
    );

    @Operation(summary = "홈 캐러셀 도서 목록", description = "흔적이 남은 도서를 대목/흔적 수와 함께 조회합니다. (FR-HOME-01,02)")
    ResponseEntity<DataResponse<BookActivityListResponse>> getHomeCarouselBooks(
            @Parameter(description = "페이지 번호 (0부터 시작)") int page,
            @Parameter(description = "페이지 크기") int size
    );

    @Operation(summary = "내가 최근에 남긴 도서 목록", description = "현재 로그인한 사용자가 최근에 대목을 남긴 도서 목록입니다. (FR-WRITE-01)")
    ResponseEntity<DataResponse<BookListResponse>> getRecentBooks(
            @Parameter(description = "페이지 번호 (0부터 시작)") int page,
            @Parameter(description = "페이지 크기") int size
    );

    @Operation(summary = "흔적 많은 도서 목록", description = "서비스 전체에서 흔적(Opinion)이 많은 순으로 도서를 조회합니다. (FR-WRITE-01)")
    ResponseEntity<DataResponse<BookActivityListResponse>> getPopularBooks(
            @Parameter(description = "페이지 번호 (0부터 시작)") int page,
            @Parameter(description = "페이지 크기") int size
    );
}
