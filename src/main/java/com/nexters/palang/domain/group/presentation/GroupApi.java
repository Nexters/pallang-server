package com.nexters.palang.domain.group.presentation;

import com.nexters.palang.domain.group.presentation.dto.CreateGroupRequest;
import com.nexters.palang.domain.group.presentation.dto.GroupDetailResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupInvitationPreviewResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupInviteLinkResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupListResponse;
import com.nexters.palang.domain.group.presentation.dto.UpdateGroupRequest;
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

@Tag(name = "Group", description = "모임 API")
public interface GroupApi {

    @Operation(summary = "모임 생성", description = "책 1권과 함께 모임을 만듭니다. 생성한 사용자는 모임장(HOST)으로 자동 가입됩니다. "
            + "독서 기간은 선택이며, 넣는다면 시작일·종료일을 함께 넣고 둘 다 오늘(한국 시간) 이후여야 합니다. "
            + "Idempotency-Key 헤더를 보내면 같은 키로 다시 요청해도 모임을 새로 만들지 않고 처음 만든 모임을 돌려줍니다 "
            + "(응답 유실·이중 탭 대비). Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공(같은 Idempotency-Key 재요청이면 처음 만든 모임)"),
            @ApiResponse(responseCode = "400", description = "필수값 누락, 인원이 2~10명 범위를 벗어남(COMMON_400_1), "
                    + "Idempotency-Key가 비었거나 100자 초과(COMMON_400_2), 시작일이 종료일보다 늦음(GROUP_400_1), "
                    + "기간 한쪽만 입력(GROUP_400_4), 새 날짜가 오늘 이전(GROUP_400_5), 모임명 길이 위반(GROUP_400_6)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_1: 필수값 누락/인원 범위 초과",
                                    value = "{\"type\":\"/api/groups\",\"title\":\"COMMON_400_1\","
                                            + "\"status\":400,\"detail\":\"인원은 최소 2명 이상이어야 합니다.\"}"),
                            @ExampleObject(name = "GROUP_400_1: 시작일이 종료일보다 늦음",
                                    value = "{\"type\":\"/api/groups\",\"title\":\"GROUP_400_1\","
                                            + "\"status\":400,\"detail\":\"시작일은 종료일보다 늦을 수 없습니다.\"}"),
                            @ExampleObject(name = "GROUP_400_4: 기간 한쪽만 입력",
                                    value = "{\"type\":\"/api/groups\",\"title\":\"GROUP_400_4\","
                                            + "\"status\":400,\"detail\":\"시작일과 종료일은 함께 입력하거나 함께 비워야 합니다.\"}"),
                            @ExampleObject(name = "GROUP_400_5: 새 날짜가 과거",
                                    value = "{\"type\":\"/api/groups\",\"title\":\"GROUP_400_5\","
                                            + "\"status\":400,\"detail\":\"새로 입력하거나 변경한 날짜는 오늘 이후여야 합니다.\"}"),
                            @ExampleObject(name = "GROUP_400_6: 모임명 길이",
                                    value = "{\"type\":\"/api/groups\",\"title\":\"GROUP_400_6\","
                                            + "\"status\":400,\"detail\":\"모임명은 공백을 제외하고 1자 이상 15자 이하여야 합니다.\"}")
                    })),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 도서를 찾을 수 없음 (BOOK_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups\",\"title\":\"BOOK_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 도서를 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<GroupDetailResponse>> createGroup(
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER,
                    description = "요청 키(선택, 최대 100자). 같은 키의 재요청은 처음 만든 모임을 돌려준다.")
            String idempotencyKey,
            @Valid CreateGroupRequest request);

    @Operation(summary = "내 모임 목록", description = "현재 가입 중인 모임을 최근 생성 순으로 조회합니다(합쳐진 모임, 나간 모임 제외). "
            + "카드에는 readingBookCount(\"총 N권 읽는 중\"), latestReadingEndDate와 readingScheduleStatus(날짜 칸), "
            + "memberPreviews/extraMemberCount(아바타, \"+N\")를 쓰세요. bookId/bookTitle/startDate/endDate/ended는 "
            + "배포된 앱 호환용으로 대표 책(가장 먼저 등록된 책) 기준입니다. Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}")))
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
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "모임원이 아님 (GROUP_403_2)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1\",\"title\":\"GROUP_403_2\","
                                    + "\"status\":403,\"detail\":\"모임원만 조회할 수 있습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 모임을 찾을 수 없음 (GROUP_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1\",\"title\":\"GROUP_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 모임을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<GroupDetailResponse>> getGroupDetail(
            @Parameter(description = "모임 ID", required = true) Long groupId
    );

    @Operation(summary = "방 설정 변경", description = "모임장만 이름/인원을 수정할 수 있습니다. 책은 대상이 아닙니다(책 추가/책 설정 "
            + "API 사용). 인원은 현재 참여 인원보다 적게 줄일 수 없습니다. 설정 화면을 열 때 받은 모임 상세의 version을 함께 "
            + "보내면, 그사이 다른 곳에서 설정이 바뀐 경우 저장하지 않고 409(GROUP_409_5)를 돌려줍니다 — 최신 값을 다시 조회해 "
            + "사용자 입력과 비교한 뒤 다시 저장하세요. 기간(startDate/endDate)은 기존 앱 호환용으로, 둘 다 보내면 "
            + "대표 책(가장 먼저 등록된 책)의 기간을 바꾸고 둘 다 비우면 기간은 그대로 둡니다. 바꾸지 않은 기존 과거 날짜는 "
            + "그대로 저장할 수 있지만 새로 바꾼 날짜는 오늘(한국 시간) 이후여야 합니다. 한 필드라도 잘못되면 아무것도 "
            + "저장되지 않습니다. Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락, 인원이 2~10명 범위를 벗어남(COMMON_400_1), "
                    + "시작일이 종료일보다 늦음(GROUP_400_1), 기간 한쪽만 입력(GROUP_400_4), 바꾼 날짜가 오늘 이전(GROUP_400_5), "
                    + "모임명 길이 위반(GROUP_400_6)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_1: 필수값 누락/인원 범위 초과",
                                    value = "{\"type\":\"/api/groups/1\",\"title\":\"COMMON_400_1\","
                                            + "\"status\":400,\"detail\":\"인원은 최대 10명까지 가능합니다.\"}"),
                            @ExampleObject(name = "GROUP_400_1: 시작일이 종료일보다 늦음",
                                    value = "{\"type\":\"/api/groups/1\",\"title\":\"GROUP_400_1\","
                                            + "\"status\":400,\"detail\":\"시작일은 종료일보다 늦을 수 없습니다.\"}"),
                            @ExampleObject(name = "GROUP_400_4: 기간 한쪽만 입력",
                                    value = "{\"type\":\"/api/groups/1\",\"title\":\"GROUP_400_4\","
                                            + "\"status\":400,\"detail\":\"시작일과 종료일은 함께 입력하거나 함께 비워야 합니다.\"}"),
                            @ExampleObject(name = "GROUP_400_5: 새 날짜가 과거",
                                    value = "{\"type\":\"/api/groups/1\",\"title\":\"GROUP_400_5\","
                                            + "\"status\":400,\"detail\":\"새로 입력하거나 변경한 날짜는 오늘 이후여야 합니다.\"}"),
                            @ExampleObject(name = "GROUP_400_6: 모임명 길이",
                                    value = "{\"type\":\"/api/groups/1\",\"title\":\"GROUP_400_6\","
                                            + "\"status\":400,\"detail\":\"모임명은 공백을 제외하고 1자 이상 15자 이하여야 합니다.\"}")
                    })),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "모임장이 아님 (GROUP_403_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1\",\"title\":\"GROUP_403_1\","
                                    + "\"status\":403,\"detail\":\"모임장만 가능한 작업입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 모임을 찾을 수 없음 (GROUP_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1\",\"title\":\"GROUP_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 모임을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "인원을 현재 참여 인원보다 적게 설정(GROUP_409_1) 또는 "
                    + "그사이 다른 곳에서 설정이 변경됨(GROUP_409_5)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "GROUP_409_1: 정원이 현재 인원보다 적음",
                                    value = "{\"type\":\"/api/groups/1\",\"title\":\"GROUP_409_1\","
                                            + "\"status\":409,\"detail\":\"인원을 현재 참여 인원보다 적게 설정할 수 없습니다.\"}"),
                            @ExampleObject(name = "GROUP_409_5: 설정 충돌",
                                    value = "{\"type\":\"/api/groups/1\",\"title\":\"GROUP_409_5\","
                                            + "\"status\":409,\"detail\":\"다른 곳에서 모임 설정이 먼저 변경되었습니다. "
                                            + "최신 설정을 확인한 뒤 다시 저장해 주세요.\"}")
                    }))
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
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "모임장이 아님 (GROUP_403_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1\",\"title\":\"GROUP_403_1\","
                                    + "\"status\":403,\"detail\":\"모임장만 가능한 작업입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 모임을 찾을 수 없음 (GROUP_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1\",\"title\":\"GROUP_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 모임을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<Void>> deleteGroup(
            @Parameter(description = "모임 ID", required = true) Long groupId
    );

    @Operation(summary = "초대 링크 조회", description = "모임장만 조회할 수 있습니다. 명단의 '초대 링크 보내기'와 목록 더보기의 "
            + "초대가 같은 API를 씁니다. 초대 코드는 별도 만료 시각이 없고 모임 기간과도 무관하게 계속 유효합니다. "
            + "full이 true면(정원 가득) 공유 전에 정원 안내를 보여주세요 — 링크를 받은 사람도 가입 시점에 정원을 다시 "
            + "확인합니다. 공유만으로는 가입되지 않으며, 받은 사람이 로그인 후 직접 참여해야 합니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1/invite-link\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "모임장이 아님 (GROUP_403_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1/invite-link\",\"title\":\"GROUP_403_1\","
                                    + "\"status\":403,\"detail\":\"모임장만 가능한 작업입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 모임을 찾을 수 없음 (GROUP_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1/invite-link\",\"title\":\"GROUP_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 모임을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<GroupInviteLinkResponse>> getInviteLink(
            @Parameter(description = "모임 ID", required = true) Long groupId
    );

    @Operation(summary = "초대 링크 재발급", description = "모임장만 재발급할 수 있습니다. 재발급하면 기존 초대 코드는 즉시 무효화됩니다. "
            + "내보낸 멤버는 내보낸 이후에 발급된 초대로만 다시 참여할 수 있으므로, 다시 들이려면 재발급한 링크를 보내야 합니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1/invite-link/regenerate\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "모임장이 아님 (GROUP_403_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1/invite-link/regenerate\",\"title\":\"GROUP_403_1\","
                                    + "\"status\":403,\"detail\":\"모임장만 가능한 작업입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 모임을 찾을 수 없음 (GROUP_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1/invite-link/regenerate\",\"title\":\"GROUP_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 모임을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<GroupInviteLinkResponse>> regenerateInviteLink(
            @Parameter(description = "모임 ID", required = true) Long groupId
    );

    @Operation(summary = "초대 링크 미리보기", description = "초대 링크를 열었을 때 가입 전 모임 정보를 보여줍니다. 인증 불필요. "
            + "Authorization: Bearer {accessToken} 헤더를 보내면 alreadyJoined(이미 가입 여부)도 함께 내려줍니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "유효하지 않은 초대 코드 (GROUP_404_2)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/invitations/abc123\",\"title\":\"GROUP_404_2\","
                                    + "\"status\":404,\"detail\":\"유효하지 않은 초대 코드입니다.\"}")))
    })
    ResponseEntity<DataResponse<GroupInvitationPreviewResponse>> previewInvitation(
            @Parameter(description = "초대 코드", required = true) String inviteCode
    );

    @Operation(summary = "초대 링크로 모임 가입", description = "정원이 가득 찼거나 이미 가입한 모임이면 가입할 수 없습니다. "
            + "모임 기간이 지났어도 가입할 수 있습니다. 이 모임에서 내보내진 계정은 내보낸 이후에 새로 발급된 초대 링크로만 "
            + "다시 참여할 수 있습니다(GROUP_403_3). Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가입 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/invitations/abc123/join\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "내보내진 계정이 내보내기 전에 발급된 초대로 재가입 시도 (GROUP_403_3)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/invitations/abc123/join\",\"title\":\"GROUP_403_3\","
                                    + "\"status\":403,\"detail\":\"이 초대 링크로는 다시 참여할 수 없습니다. "
                                    + "모임장에게 새 초대 링크를 요청해 주세요.\"}"))),
            @ApiResponse(responseCode = "404", description = "유효하지 않은 초대 코드 (GROUP_404_2)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/invitations/abc123/join\",\"title\":\"GROUP_404_2\","
                                    + "\"status\":404,\"detail\":\"유효하지 않은 초대 코드입니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 가입된 모임 (GROUP_409_2) 또는 정원이 가득 참 (GROUP_409_3)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "GROUP_409_2: 이미 가입된 모임",
                                    value = "{\"type\":\"/api/groups/invitations/abc123/join\",\"title\":\"GROUP_409_2\","
                                            + "\"status\":409,\"detail\":\"이미 가입된 모임입니다.\"}"),
                            @ExampleObject(name = "GROUP_409_3: 정원 초과",
                                    value = "{\"type\":\"/api/groups/invitations/abc123/join\",\"title\":\"GROUP_409_3\","
                                            + "\"status\":409,\"detail\":\"모임 정원이 가득 찼습니다.\"}")
                    }))
    })
    ResponseEntity<DataResponse<GroupDetailResponse>> joinGroup(
            @Parameter(description = "초대 코드", required = true) String inviteCode
    );
}
