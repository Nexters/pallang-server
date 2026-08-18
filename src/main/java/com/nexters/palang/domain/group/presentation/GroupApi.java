package com.nexters.palang.domain.group.presentation;

import com.nexters.palang.domain.group.presentation.dto.CreateGroupRequest;
import com.nexters.palang.domain.group.presentation.dto.GroupDetailResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupListResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupMemberListResponse;
import com.nexters.palang.domain.group.presentation.dto.UpdateGroupRequest;
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
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Group", description = "모임 API")
public interface GroupApi {

    @Operation(summary = "모임 생성", description = "책 1권을 고정해 모임을 만듭니다. 생성한 사용자는 모임장(HOST)으로 자동 가입됩니다. "
            + "책은 생성 이후 변경할 수 없습니다. Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락, 인원이 2~10명 범위를 벗어남(COMMON_400_1) "
                    + "또는 시작일이 종료일보다 늦음 (GROUP_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 도서를 찾을 수 없음 (BOOK_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<GroupDetailResponse>> createGroup(@Valid CreateGroupRequest request);

    @Operation(summary = "내 모임 목록", description = "현재 로그인한 사용자가 속한 모임을 최근 생성 순으로 조회합니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<GroupListResponse>> getMyGroups(
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "모임 상세 조회", description = "모임원만 조회할 수 있습니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "모임원이 아님 (GROUP_403_2)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 모임을 찾을 수 없음 (GROUP_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1\",\"title\":\"GROUP_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 모임을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<GroupDetailResponse>> getGroupDetail(
            @Parameter(description = "모임 ID", required = true) Long groupId
    );

    @Operation(summary = "방 설정 변경", description = "모임장만 이름/인원/기간을 수정할 수 있습니다. 책은 대상이 아닙니다. "
            + "인원은 현재 참여 인원보다 적게 줄일 수 없습니다. Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락, 인원이 2~10명 범위를 벗어남(COMMON_400_1) "
                    + "또는 시작일이 종료일보다 늦음 (GROUP_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "모임장이 아님 (GROUP_403_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 모임을 찾을 수 없음 (GROUP_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "인원을 현재 참여 인원보다 적게 설정 (GROUP_409_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<GroupDetailResponse>> updateGroup(
            @Parameter(description = "모임 ID", required = true) Long groupId,
            @Valid UpdateGroupRequest request
    );

    @Operation(summary = "모임 삭제", description = "모임장만 모임을 삭제할 수 있습니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "모임장이 아님 (GROUP_403_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 모임을 찾을 수 없음 (GROUP_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<Void>> deleteGroup(
            @Parameter(description = "모임 ID", required = true) Long groupId
    );

    @Operation(summary = "모임 멤버 목록", description = "모임원만 조회할 수 있습니다. 모임장이 먼저, 그다음 가입 순으로 정렬됩니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "모임원이 아님 (GROUP_403_2)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 모임을 찾을 수 없음 (GROUP_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<GroupMemberListResponse>> getGroupMembers(
            @Parameter(description = "모임 ID", required = true) Long groupId,
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );
}
