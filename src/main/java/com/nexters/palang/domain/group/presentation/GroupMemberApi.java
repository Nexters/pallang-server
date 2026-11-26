package com.nexters.palang.domain.group.presentation;

import com.nexters.palang.domain.group.presentation.dto.GroupMemberListResponse;
import com.nexters.palang.domain.group.presentation.dto.RemoveGroupMemberRequest;
import com.nexters.palang.global.common.error.ErrorResponse;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Group Member", description = "모임 멤버 명단 / 내보내기 API")
public interface GroupMemberApi {

    @Operation(summary = "모임 멤버 목록", description = "현재 모임원만 조회할 수 있습니다. 현재 멤버만 보이며, 모임장이 먼저, "
            + "그다음 참여일 순(같으면 userId 순)으로 정렬됩니다. 모임장/본인 여부(isHost/isMe)는 계정 ID로 판별하며, 본인이 "
            + "모임장이면 같은 행에 둘 다 true입니다. memberCount/capacity는 페이지와 무관한 전체 기준(명단 제목 \"5 / 10명\")입니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1/members\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "현재 모임원이 아님 (GROUP_403_2)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1/members\",\"title\":\"GROUP_403_2\","
                                    + "\"status\":403,\"detail\":\"모임원만 조회할 수 있습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 모임을 찾을 수 없음 (GROUP_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1/members\",\"title\":\"GROUP_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 모임을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<GroupMemberListResponse>> getGroupMembers(
            @Parameter(description = "모임 ID", required = true) Long groupId,
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "멤버 내보내기", description = "모임장만 내보낼 수 있습니다. 사유는 1개 필수이며, OTHER(기타)일 때만 "
            + "otherText(공백 제외 1~200자)를 보냅니다. 모임장 자신은 내보낼 수 없습니다. 대상이 이미 나갔거나 내보내졌으면 "
            + "아무것도 바꾸지 않고 최신 명단을 돌려줍니다. 성공하면 대상은 이 모임의 명단/인원/접근권에서 빠지고(이후 조회·작성 "
            + "요청은 GROUP_403_2), 대상이 남긴 기록과 개인 서재는 그대로 남습니다. 대상에게는 참여 종료 사실만 알림으로 "
            + "알리며 사유는 공개하지 않습니다. 내보내진 계정은 내보낸 이후에 새로 발급된 초대로만 다시 참여할 수 있습니다. "
            + "응답은 내보낸 뒤의 최신 명단(첫 페이지)입니다. Idempotency-Key를 보내면 같은 키의 재요청은 다시 실행하지 않습니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내보내기 성공(또는 이미 나간 대상) — 최신 명단"),
            @ApiResponse(responseCode = "400", description = "사유 누락(COMMON_400_1), 사유/기타 내용 형식 위반(GROUP_400_7), "
                    + "Idempotency-Key 형식(COMMON_400_2)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1/members/2/removal\",\"title\":\"GROUP_400_7\","
                                    + "\"status\":400,\"detail\":\"내보내기 사유를 하나 선택해 주세요. 기타를 고르면 내용을 "
                                    + "1자 이상 200자 이하로 입력해야 합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "모임장이 아님 (GROUP_403_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "모임 없음(GROUP_404_1) 또는 이 모임의 멤버였던 적이 없음(GROUP_404_4)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "모임장(자기 자신)은 내보낼 수 없음 (GROUP_409_6)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1/members/1/removal\",\"title\":\"GROUP_409_6\","
                                    + "\"status\":409,\"detail\":\"모임장은 내보낼 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<GroupMemberListResponse>> removeMember(
            @Parameter(description = "모임 ID", required = true) Long groupId,
            @Parameter(description = "내보낼 멤버의 userId", required = true) Long userId,
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER,
                    description = "요청 키(선택, 최대 100자). 같은 키의 재요청은 다시 실행하지 않는다.")
            String idempotencyKey,
            @Valid RemoveGroupMemberRequest request
    );
}
