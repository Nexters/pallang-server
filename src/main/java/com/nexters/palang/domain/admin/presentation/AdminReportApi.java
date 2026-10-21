package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.application.AdminSortOrder;
import com.nexters.palang.domain.admin.presentation.dto.AdminReportListResponse;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin", description = "관리자 전용 API — 테스트 계정 정리 등")
public interface AdminReportApi {

    @Operation(summary = "관리자 신고 목록", description = "신고를 대상(의견/댓글) 단위로 묶어 가장 최근에 신고된 대상부터 "
            + "보여줍니다. 대상이 이미 삭제됐으면 targetAlive=false입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)")
    })
    ResponseEntity<DataResponse<AdminReportListResponse>> getReportedTargets(
            @Parameter(description = "정렬 — 마지막 신고 시각 기준 (LATEST: 최신순, 기본값 / OLDEST: 오래된순)") AdminSortOrder sort,
            @Parameter(hidden = true) int page,
            @Parameter(hidden = true) int size
    );
}
