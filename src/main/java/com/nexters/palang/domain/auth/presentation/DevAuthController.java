package com.nexters.palang.domain.auth.presentation;

import com.nexters.palang.domain.auth.application.AuthMapper;
import com.nexters.palang.domain.auth.application.AuthResult;
import com.nexters.palang.domain.auth.application.AuthService;
import com.nexters.palang.domain.auth.presentation.dto.DevLoginRequest;
import com.nexters.palang.domain.auth.presentation.dto.LoginResponse;
import com.nexters.palang.global.common.error.ErrorResponse;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// local 프로파일에서만 활성화된다 — 운영 환경에는 이 엔드포인트 자체가 존재하지 않는다.
// 카카오 서버를 타지 않고 바로 우리 서비스 JWT를 발급해, 스웨거에서 "인증 필요" API를 빠르게 테스트할 수 있게 한다.
@Profile("local")
@Tag(name = "Auth (Dev)", description = "[개발용] local 프로파일 전용 — 실제 카카오 로그인 없이 JWT를 발급합니다. 운영에는 노출되지 않습니다.")
@RestController
@RequiredArgsConstructor
public class DevAuthController {

    private final AuthService authService;

    @Operation(summary = "[개발용] 임의 유저로 즉시 로그인", description = "카카오 서버를 호출하지 않고 바로 JWT를 발급합니다. "
            + "userId를 주면 해당 유저로 로그인하고, 생략하면 새 테스트 유저를 만들어 로그인 처리합니다. "
            + "local 프로파일에서만 등록되는 개발 편의용 엔드포인트입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 userId (USER_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/auth/dev-login\",\"title\":\"USER_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 사용자를 찾을 수 없습니다.\"}")))
    })
    @PostMapping("/api/auth/dev-login")
    public ResponseEntity<DataResponse<LoginResponse>> devLogin(
            @RequestBody(required = false) DevLoginRequest request) {
        Long userId = request != null ? request.userId() : null;
        AuthResult result = authService.devLogin(userId);
        return ResponseEntity.ok(DataResponse.from(AuthMapper.toLoginResponse(result)));
    }
}
