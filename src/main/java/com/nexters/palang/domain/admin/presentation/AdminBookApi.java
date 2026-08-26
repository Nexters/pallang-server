package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.presentation.dto.AdminBookListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminBookSummaryResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminUpdateBookRequest;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin", description = "관리자 전용 API — 테스트 계정 정리 등")
public interface AdminBookApi {

    @Operation(summary = "관리자 도서 검색", description = "제목 또는 저자에 키워드가 포함된 도서를 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)")
    })
    ResponseEntity<DataResponse<AdminBookListResponse>> searchBooks(
            @Parameter(description = "제목/저자 검색어") String keyword,
            @Parameter(hidden = true) int page,
            @Parameter(hidden = true) int size
    );

    @Operation(summary = "관리자 도서 수정", description = "제목/저자/출판사/쪽수/ISBN/표지 URL을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)"),
            @ApiResponse(responseCode = "404", description = "해당 도서를 찾을 수 없음 (BOOK_404_1)")
    })
    ResponseEntity<DataResponse<AdminBookSummaryResponse>> updateBook(Long bookId, AdminUpdateBookRequest request);

    @Operation(summary = "관리자 도서 삭제", description = "이 책으로 만들어진 모임과 그 안의 대목/의견/댓글/"
            + "데코/좋아요/모임원, 모임에 속하지 않은 대목까지 전부 하드 삭제한 뒤 책 자체를 지웁니다. "
            + "여러 사용자의 데이터에 영향을 줄 수 있는 위험한 작업이며 되돌릴 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)"),
            @ApiResponse(responseCode = "404", description = "해당 도서를 찾을 수 없음 (BOOK_404_1)")
    })
    ResponseEntity<DataResponse<Void>> deleteBook(Long bookId);
}
