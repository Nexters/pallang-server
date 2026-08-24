package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.presentation.dto.AdminUserListResponse;
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

// 관리자 페이지(/admin) 전용 API. 일반 API 문서와 섞이지 않도록 별도 태그로 분리한다.
@Tag(name = "Admin", description = "관리자 전용 API — 테스트 계정 정리 등")
public interface AdminUserApi {

    @Operation(summary = "관리자 유저 검색", description = "닉네임 또는 이메일에 키워드가 포함된 유저를 검색합니다. "
            + "관리자 화이트리스트(admin.user-ids)에 속한 계정의 JWT로만 호출할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/admin/users\",\"title\":\"ADMIN_403_1\","
                                    + "\"status\":403,\"detail\":\"관리자 권한이 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<AdminUserListResponse>> searchUsers(
            @Parameter(description = "닉네임/이메일 검색어") String keyword,
            @Parameter(hidden = true) int page,
            @Parameter(hidden = true) int size
    );

    @Operation(summary = "관리자 유저 삭제", description = "유저와 연관 데이터(모임/흔적/의견/댓글/좋아요/데코 등)를 "
            + "하드 삭제합니다. 되돌릴 수 없습니다. 다른 멤버가 있는 모임의 호스트이거나, 다른 사용자의 의견이 "
            + "달린 흔적을 작성한 경우 409로 차단됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 (ADMIN_403_1)"),
            @ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없음 (USER_404_1)"),
            @ApiResponse(responseCode = "409", description = "다른 유저의 데이터와 얽혀 있어 삭제할 수 없음 "
                    + "(ADMIN_409_1 / ADMIN_409_2)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/admin/users/42\",\"title\":\"ADMIN_409_1\","
                                    + "\"status\":409,\"detail\":\"다른 멤버가 있는 모임의 호스트여서 삭제할 수 "
                                    + "없습니다: 독서모임A\"}")))
    })
    ResponseEntity<DataResponse<Void>> deleteUser(Long userId);
}
