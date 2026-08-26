package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.presentation.dto.AdminGroupListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminGroupSummaryResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminUpdateGroupRequest;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin", description = "관리자 전용 API — 테스트 계정 정리 등")
public interface AdminGroupApi {

    @Operation(summary = "관리자 모임 검색", description = "모임명에 키워드가 포함된 모임을 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)")
    })
    ResponseEntity<DataResponse<AdminGroupListResponse>> searchGroups(
            @Parameter(description = "모임명 검색어") String keyword,
            @Parameter(hidden = true) int page,
            @Parameter(hidden = true) int size
    );

    @Operation(summary = "관리자 모임 수정", description = "모임명/정원/시작일/종료일을 수정합니다. "
            + "도서는 모임 생성 후 바꿀 수 없어 수정 대상이 아닙니다. 정원을 현재 참여 인원보다 적게 "
            + "설정하면 409로 거절됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 정원/기간 (GROUP_400_1 / GROUP_400_2)"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)"),
            @ApiResponse(responseCode = "404", description = "해당 모임을 찾을 수 없음 (GROUP_404_1)"),
            @ApiResponse(responseCode = "409", description = "정원이 현재 참여 인원보다 적음 (GROUP_409_1)")
    })
    ResponseEntity<DataResponse<AdminGroupSummaryResponse>> updateGroup(
            Long groupId, AdminUpdateGroupRequest request);

    @Operation(summary = "관리자 모임 삭제", description = "모임과 그 안의 대목/의견/댓글/데코/좋아요, "
            + "모임원까지 전부 하드 삭제합니다. 되돌릴 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)"),
            @ApiResponse(responseCode = "404", description = "해당 모임을 찾을 수 없음 (GROUP_404_1)")
    })
    ResponseEntity<DataResponse<Void>> deleteGroup(Long groupId);
}
