package com.nexters.palang.domain.notification.presentation;

import com.nexters.palang.domain.notification.presentation.dto.NotificationListResponse;
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

@Tag(name = "Notification", description = "알림 API")
public interface NotificationApi {

    @Operation(summary = "알림 목록 조회",
            description = "내가 받은 알림을 최신순으로 페이지네이션 조회합니다. "
                    + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/notifications\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}")))
    })
    ResponseEntity<DataResponse<NotificationListResponse>> getNotifications(
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "알림 단건 읽음 처리", description = "본인이 받은 알림 하나를 읽음 상태로 변경합니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/notifications/1/read\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "본인에게 온 알림이 아니거나 존재하지 않음 (NOTIFICATION_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/notifications/1/read\",\"title\":\"NOTIFICATION_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 알림을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<Void>> readNotification(
            @Parameter(description = "알림 ID", required = true) Long notificationId
    );

    @Operation(summary = "알림 전체 읽음 처리", description = "본인이 받은 안 읽은 알림을 모두 읽음 상태로 변경합니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/notifications/read-all\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}")))
    })
    ResponseEntity<DataResponse<Void>> readAllNotifications();
}
