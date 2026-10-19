package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.presentation.dto.AdminOpinionListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminOpinionSummaryResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminUpdateOpinionRequest;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin", description = "관리자 전용 API — 테스트 계정 정리 등")
public interface AdminOpinionApi {

    @Operation(summary = "관리자 의견 검색", description = "내용에 키워드가 포함된 의견을 검색합니다. "
            + "passageId를 주면 키워드 검색 대신 그 대목에 달린 의견 전부를 작성 순서(오래된 순)로 반환합니다 "
            + "— 관리자 페이지에서 대목 행을 펼쳐볼 때 사용합니다. 소프트 삭제된 의견도 함께 조회됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)")
    })
    ResponseEntity<DataResponse<AdminOpinionListResponse>> searchOpinions(
            @Parameter(description = "이 대목의 의견만 작성 순서대로 조회 (주어지면 keyword 무시)") Long passageId,
            @Parameter(description = "내용 검색어 (passageId가 없을 때만 사용)") String keyword,
            @Parameter(hidden = true) int page,
            @Parameter(hidden = true) int size
    );

    @Operation(summary = "관리자 의견 수정", description = "내용을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)"),
            @ApiResponse(responseCode = "404", description = "해당 의견을 찾을 수 없음 (OPINION_404_1)")
    })
    ResponseEntity<DataResponse<AdminOpinionSummaryResponse>> updateOpinion(
            Long opinionId, AdminUpdateOpinionRequest request);

    @Operation(summary = "관리자 의견 삭제", description = "의견과 거기 달린 댓글/데코/좋아요까지 전부 "
            + "하드 삭제합니다. 그 대목에 남은 의견이 없으면 대목도 함께 소프트 삭제됩니다. 되돌릴 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)"),
            @ApiResponse(responseCode = "404", description = "해당 의견을 찾을 수 없음 (OPINION_404_1)")
    })
    ResponseEntity<DataResponse<Void>> deleteOpinion(Long opinionId);
}
