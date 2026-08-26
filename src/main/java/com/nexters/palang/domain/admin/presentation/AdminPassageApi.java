package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.presentation.dto.AdminPassageListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminPassageSummaryResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminUpdatePassageRequest;
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
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin", description = "관리자 전용 API — 테스트 계정 정리 등")
public interface AdminPassageApi {

    @Operation(summary = "관리자 대목 검색", description = "인용문에 키워드가 포함된 대목을 검색합니다. "
            + "소프트 삭제된 대목도 함께 조회됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)")
    })
    ResponseEntity<DataResponse<AdminPassageListResponse>> searchPassages(
            @Parameter(description = "인용문 검색어") String keyword,
            @Parameter(hidden = true) int page,
            @Parameter(hidden = true) int size
    );

    @Operation(summary = "관리자 대목 수정", description = "인용문/페이지 번호/스포일러 여부를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)"),
            @ApiResponse(responseCode = "404", description = "해당 대목을 찾을 수 없음 (PASSAGE_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/admin/passages/1\",\"title\":\"PASSAGE_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 대목을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<AdminPassageSummaryResponse>> updatePassage(
            Long passageId, AdminUpdatePassageRequest request);

    @Operation(summary = "관리자 대목 삭제", description = "대목과 거기 달린 의견/댓글/데코/좋아요까지 전부 "
            + "하드 삭제합니다. 되돌릴 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)"),
            @ApiResponse(responseCode = "404", description = "해당 대목을 찾을 수 없음 (PASSAGE_404_1)")
    })
    ResponseEntity<DataResponse<Void>> deletePassage(Long passageId);
}
