package com.nexters.palang.domain.block.presentation;

import com.nexters.palang.domain.block.presentation.dto.BlockedUserListResponse;
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

@Tag(name = "Block", description = "사용자 차단 API")
public interface BlockApi {

    @Operation(summary = "사용자 차단", description = "해당 사용자를 차단합니다. 차단하면 이후 흔적 목록에서 차단한 사용자의 흔적이 보이지 않습니다. "
            + "본인은 차단할 수 없습니다. Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "차단 성공"),
            @ApiResponse(responseCode = "400", description = "본인 차단 시도 (BLOCK_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/1/block\",\"title\":\"BLOCK_400_1\","
                                    + "\"status\":400,\"detail\":\"본인을 차단할 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/1/block\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없음 (USER_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/1/block\",\"title\":\"USER_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 사용자를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 차단한 사용자 (BLOCK_409_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/1/block\",\"title\":\"BLOCK_409_1\","
                                    + "\"status\":409,\"detail\":\"이미 차단한 사용자입니다.\"}")))
    })
    ResponseEntity<DataResponse<Void>> block(
            @Parameter(description = "차단할 사용자 ID", required = true) Long userId
    );

    @Operation(summary = "사용자 차단 해제", description = "차단했던 사용자를 차단 해제합니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "차단 해제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/1/block\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "차단 내역을 찾을 수 없음 (BLOCK_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/1/block\",\"title\":\"BLOCK_404_1\","
                                    + "\"status\":404,\"detail\":\"차단 내역을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<Void>> unblock(
            @Parameter(description = "차단 해제할 사용자 ID", required = true) Long userId
    );

    @Operation(summary = "차단 목록 조회", description = "내가 차단한 사용자 목록을 최신순으로 조회합니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "page/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/blocks\",\"title\":\"COMMON_400_1\","
                                    + "\"status\":400,\"detail\":\"'size' 파라미터의 값이 올바르지 않습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/blocks\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}")))
    })
    ResponseEntity<DataResponse<BlockedUserListResponse>> getBlockedUsers(
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );
}
