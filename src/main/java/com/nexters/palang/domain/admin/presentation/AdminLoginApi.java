package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.presentation.dto.AdminLoginRequest;
import com.nexters.palang.domain.admin.presentation.dto.AdminLoginResponse;
import com.nexters.palang.global.common.error.ErrorResponse;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin", description = "관리자 전용 API — 테스트 계정 정리 등")
public interface AdminLoginApi {

    @Operation(summary = "관리자 로그인", description = "일반 유저 로그인과 별개인 관리자 전용 로그인이다. "
            + "admin.login-username/password(환경변수 ADMIN_LOGIN_USERNAME/ADMIN_LOGIN_PASSWORD)와 "
            + "정확히 일치해야 하며, 발급된 토큰은 다른 관리자 API 호출 시 Authorization: Bearer로 사용한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "아이디/비밀번호 불일치 (ADMIN_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/admin/auth/login\",\"title\":\"ADMIN_401_1\","
                                    + "\"status\":401,\"detail\":\"아이디 또는 비밀번호가 올바르지 않습니다.\"}")))
    })
    ResponseEntity<DataResponse<AdminLoginResponse>> login(AdminLoginRequest request);
}
