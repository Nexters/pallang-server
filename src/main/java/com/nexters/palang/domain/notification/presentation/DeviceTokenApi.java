package com.nexters.palang.domain.notification.presentation;

import com.nexters.palang.domain.notification.presentation.dto.RegisterDeviceTokenRequest;
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

@Tag(name = "DeviceToken", description = "FCM 디바이스 토큰 API")
public interface DeviceTokenApi {

    @Operation(summary = "디바이스 토큰 등록/갱신",
            description = "FCM 디바이스 토큰을 등록합니다. 이미 등록된 토큰이면(재설치/재로그인 등) 소유자를 현재 로그인 사용자로 갱신합니다. "
                    + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록/갱신 성공"),
            @ApiResponse(responseCode = "400", description = "토큰 누락 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/notifications/device-tokens\",\"title\":\"COMMON_400_1\","
                                    + "\"status\":400,\"detail\":\"디바이스 토큰은 필수입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/notifications/device-tokens\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}")))
    })
    ResponseEntity<DataResponse<Void>> registerDeviceToken(@Valid RegisterDeviceTokenRequest request);

    @Operation(summary = "디바이스 토큰 삭제", description = "로그아웃 등으로 더 이상 유효하지 않은 디바이스 토큰을 삭제합니다. "
            + "본인 소유가 아니거나 존재하지 않는 토큰이어도 멱등하게 200을 반환합니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/notifications/device-tokens\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}")))
    })
    ResponseEntity<DataResponse<Void>> removeDeviceToken(
            @Parameter(description = "삭제할 디바이스 토큰", required = true) String token
    );
}
