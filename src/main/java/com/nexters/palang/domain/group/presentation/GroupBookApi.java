package com.nexters.palang.domain.group.presentation;

import com.nexters.palang.domain.group.application.GroupBookStatusFilter;
import com.nexters.palang.domain.group.presentation.dto.AddGroupBookRequest;
import com.nexters.palang.domain.group.presentation.dto.GroupBookListResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupBookResponse;
import com.nexters.palang.domain.group.presentation.dto.UpdateGroupBookRequest;
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

@Tag(name = "Group Book", description = "모임 책 API (함께 읽는 책 / 완독한 책)")
public interface GroupBookApi {

    @Operation(summary = "모임 책 목록", description = "현재 모임원만 조회할 수 있습니다. 등록 시각 내림차순(새 책이 맨 위)으로 "
            + "정렬됩니다. status를 비우면 기본 탭(함께 읽는 책, 0권이면 완독한 책)을 골라 응답의 status로 알려줍니다. "
            + "완독 여부는 조회 시점 기준으로 서버가 계산합니다(종료일 다음 날 0시(한국 시간)부터 완독, 모임장이 직접 고른 "
            + "상태가 있으면 그 값). 모임 설정 화면처럼 등록된 책 전체가 필요하면 status=ALL을 씁니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공(0권이어도 빈 목록과 개수를 정상 응답)"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "현재 모임원이 아님 (GROUP_403_2)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1/books\",\"title\":\"GROUP_403_2\","
                                    + "\"status\":403,\"detail\":\"모임원만 조회할 수 있습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 모임을 찾을 수 없음 (GROUP_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<GroupBookListResponse>> getGroupBooks(
            @Parameter(description = "모임 ID", required = true) Long groupId,
            @Parameter(description = "READING(함께 읽는 책) / FINISHED(완독한 책) / ALL(전체). 비우면 기본 탭")
            GroupBookStatusFilter status,
            @Parameter(description = "페이지 번호(0부터)") int page,
            @Parameter(description = "페이지 크기(1~100)") int size
    );

    @Operation(summary = "함께 읽을 책 추가", description = "모임장만 추가할 수 있습니다. 같은 판본(같은 도서 또는 같은 ISBN)이 "
            + "이미 등록돼 있으면(읽는 중이든 완독이든) 추가할 수 없습니다. 제목만 같은 다른 판본은 추가할 수 있습니다. "
            + "기간은 선택이며, 넣는다면 시작일·종료일을 함께 넣고 둘 다 오늘(한국 시간) 이후여야 합니다. 정원이 가득 차도 "
            + "책은 추가할 수 있습니다. Idempotency-Key 헤더를 보내면 같은 키의 재요청은 새로 추가하지 않고 처음 추가한 책을 "
            + "돌려줍니다. Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추가 성공(같은 Idempotency-Key 재요청이면 처음 추가한 책)"),
            @ApiResponse(responseCode = "400", description = "책 선택 누락(COMMON_400_1), Idempotency-Key 형식(COMMON_400_2), "
                    + "시작일이 종료일보다 늦음(GROUP_400_1), 기간 한쪽만 입력(GROUP_400_4), 새 날짜가 오늘 이전(GROUP_400_5)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "모임장이 아님 (GROUP_403_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "모임 없음(GROUP_404_1) 또는 도서 없음(BOOK_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 모임에 등록된 판본 (GROUP_409_4)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/groups/1/books\",\"title\":\"GROUP_409_4\","
                                    + "\"status\":409,\"detail\":\"이미 모임에 등록된 책입니다.\"}")))
    })
    ResponseEntity<DataResponse<GroupBookResponse>> addGroupBook(
            @Parameter(description = "모임 ID", required = true) Long groupId,
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER,
                    description = "요청 키(선택, 최대 100자). 같은 키의 재요청은 처음 추가한 책을 돌려준다.")
            String idempotencyKey,
            @Valid AddGroupBookRequest request
    );

    @Operation(summary = "책 설정 변경", description = "모임장만 변경할 수 있습니다. 판본은 바꿀 수 없고 기간과 독서 상태만 "
            + "바꿉니다. 기간은 저장할 값 전체를 보내며(둘 다 비우면 기간 없음), 바꾸지 않은 기존 과거 날짜는 그대로 보내도 "
            + "됩니다. 상태는 사용자가 직접 바꾼 경우에만 보내고, 보내면 날짜와 무관하게 그 상태로 고정됩니다. "
            + "기간이 바뀌면 다른 모임원에게 일정 변경 알림이 갑니다. Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "시작일이 종료일보다 늦음(GROUP_400_1), 기간 한쪽만 입력(GROUP_400_4), "
                    + "바꾼 날짜가 오늘 이전(GROUP_400_5)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "모임장이 아님 (GROUP_403_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "모임 없음(GROUP_404_1) 또는 이 모임의 책이 아님(GROUP_404_3)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<GroupBookResponse>> updateGroupBook(
            @Parameter(description = "모임 ID", required = true) Long groupId,
            @Parameter(description = "모임 책 ID(entryId)", required = true) Long entryId,
            @Valid UpdateGroupBookRequest request
    );
}
