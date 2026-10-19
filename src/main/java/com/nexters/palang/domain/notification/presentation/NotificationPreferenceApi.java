package com.nexters.palang.domain.notification.presentation;

import com.nexters.palang.domain.notification.presentation.dto.NotificationPreferenceResponse;
import com.nexters.palang.domain.notification.presentation.dto.UpdateNotificationPreferenceRequest;
import com.nexters.palang.global.common.error.ErrorResponse;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "NotificationPreference", description = "알림 설정 API")
public interface NotificationPreferenceApi {

    @Operation(summary = "알림 설정 조회", description = "푸시 알림 마스터 스위치와 활동/모임 세부 카테고리별 알림 수신 여부를 조회합니다. "
            + "설정을 한 번도 변경하지 않은 사용자는 전부 true로 반환됩니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/notifications/settings\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}")))
    })
    ResponseEntity<DataResponse<NotificationPreferenceResponse>> getSettings();

    @Operation(summary = "알림 설정 변경", description = "푸시 알림 마스터 스위치와 활동/모임 세부 카테고리별 알림 수신 여부를 변경합니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 누락 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/notifications/settings\",\"title\":\"COMMON_400_1\","
                                    + "\"status\":400,\"detail\":\"푸시 알림 설정 값은 필수입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/notifications/settings\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}")))
    })
    ResponseEntity<DataResponse<Void>> updateSettings(@Valid UpdateNotificationPreferenceRequest request);
}
