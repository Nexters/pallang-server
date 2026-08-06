package com.nexters.palang.domain.auth.presentation;

import com.nexters.palang.domain.auth.presentation.dto.AppleLoginRequest;
import com.nexters.palang.domain.auth.presentation.dto.KakaoLoginRequest;
import com.nexters.palang.domain.auth.presentation.dto.LoginResponse;
import com.nexters.palang.domain.auth.presentation.dto.RefreshTokenRequest;
import com.nexters.palang.domain.auth.presentation.dto.TermsAgreementResponse;
import com.nexters.palang.domain.auth.presentation.dto.TokenResponse;
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

@Tag(name = "Auth", description = "카카오/애플 로그인 및 인증 API")
public interface AuthApi {

    @Operation(summary = "카카오 로그인", description = "모바일 앱이 카카오 SDK로 로그인해 받은 카카오 액세스 토큰을 전달하면, "
            + "카카오 사용자 정보 API로 직접 검증한 뒤 가입/로그인을 처리하고 서비스 자체 JWT를 발급합니다. "
            + "처음 로그인하는 사용자는 닉네임이 자동 생성되며(isNewUser=true), 약관 동의는 별도로 POST /api/auth/terms를 호출해야 합니다. "
            + "탈퇴했던 계정으로 다시 로그인하면 예전 계정이 되살아나지 않고 완전히 새로운 계정으로 가입됩니다(isNewUser=true) "
            + "— 예전 계정과 거기 남긴 콘텐츠는 탈퇴 상태로 그대로 남습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인/가입 성공"),
            @ApiResponse(responseCode = "400", description = "카카오 액세스 토큰 누락 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/auth/kakao\",\"title\":\"COMMON_400_1\","
                                    + "\"status\":400,\"detail\":\"카카오 액세스 토큰은 필수입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "카카오 인증 실패, 만료/유효하지 않은 토큰 (AUTH_401_4)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/auth/kakao\",\"title\":\"AUTH_401_4\","
                                    + "\"status\":401,\"detail\":\"카카오 인증에 실패했습니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "탈퇴한 계정으로 재로그인 시도 (AUTH_403_1, 정상 흐름에서는 "
                    + "발생하지 않고 레거시 데이터에 대한 방어용)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/auth/kakao\",\"title\":\"AUTH_403_1\","
                                    + "\"status\":403,\"detail\":\"탈퇴한 계정입니다.\"}")))
    })
    ResponseEntity<DataResponse<LoginResponse>> loginWithKakao(@Valid KakaoLoginRequest request);

    @Operation(summary = "애플 로그인", description = "웹(Apple JS SDK) 또는 iOS 앱(네이티브 Sign in with Apple)에서 발급받은 "
            + "identity token(JWT)을 전달하면, Apple 공개키(JWKS)로 서명을 직접 검증한 뒤 가입/로그인을 처리하고 "
            + "서비스 자체 JWT를 발급합니다. aud 클레임은 설정된 웹 Service ID 또는 앱 Bundle ID 중 하나와 일치해야 합니다. "
            + "authorizationCode를 함께 전달하면 회원탈퇴 시 애플 연동 해제(revoke)에 쓸 refresh token을 미리 "
            + "확보해둡니다(실패해도 로그인은 계속 진행됩니다). givenName/familyName은 애플이 최초 로그인 시에만 "
            + "내려주는 값으로, 이후 로그인엔 null이어도 됩니다. "
            + "처음 로그인하는 사용자는 닉네임이 자동 생성되며(isNewUser=true), 약관 동의는 별도로 POST /api/auth/terms를 호출해야 합니다. "
            + "탈퇴했던 계정으로 다시 로그인하면 예전 계정이 되살아나지 않고 완전히 새로운 계정으로 가입됩니다(isNewUser=true) "
            + "— 예전 계정과 거기 남긴 콘텐츠는 탈퇴 상태로 그대로 남습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인/가입 성공"),
            @ApiResponse(responseCode = "400", description = "애플 identity token 누락 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/auth/apple\",\"title\":\"COMMON_400_1\","
                                    + "\"status\":400,\"detail\":\"애플 identity token은 필수입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "애플 인증 실패, 서명/발급자/대상/만료 검증 실패 (AUTH_401_6)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/auth/apple\",\"title\":\"AUTH_401_6\","
                                    + "\"status\":401,\"detail\":\"애플 인증에 실패했습니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "탈퇴한 계정으로 재로그인 시도 (AUTH_403_1, 정상 흐름에서는 "
                    + "발생하지 않고 레거시 데이터에 대한 방어용)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/auth/apple\",\"title\":\"AUTH_403_1\","
                                    + "\"status\":403,\"detail\":\"탈퇴한 계정입니다.\"}")))
    })
    ResponseEntity<DataResponse<LoginResponse>> loginWithApple(@Valid AppleLoginRequest request);

    @Operation(summary = "약관 동의", description = "이용약관 + 개인정보 수집·이용 동의를 1회 처리로 기록합니다(FR-AUTH-03). "
            + "이미 동의한 사용자가 다시 호출해도 동의 시각만 갱신되며 에러는 발생하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "동의 처리 성공"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요함 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/auth/terms\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}")))
    })
    ResponseEntity<DataResponse<TermsAgreementResponse>> agreeToTerms();

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 새 액세스 토큰 + 리프레시 토큰을 발급합니다. "
            + "기존 리프레시 토큰은 사용 즉시 폐기됩니다(재사용 불가).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "400", description = "리프레시 토큰 누락 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/auth/refresh\",\"title\":\"COMMON_400_1\","
                                    + "\"status\":400,\"detail\":\"리프레시 토큰은 필수입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "만료되었거나(AUTH_401_2) 유효하지 않은(AUTH_401_5) 리프레시 토큰",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "AUTH_401_2", value = "{\"type\":\"/api/auth/refresh\","
                                    + "\"title\":\"AUTH_401_2\",\"status\":401,\"detail\":\"토큰이 만료되었습니다.\"}"),
                            @ExampleObject(name = "AUTH_401_5", value = "{\"type\":\"/api/auth/refresh\","
                                    + "\"title\":\"AUTH_401_5\",\"status\":401,"
                                    + "\"detail\":\"유효하지 않은 리프레시 토큰입니다.\"}")
                    }))
    })
    ResponseEntity<DataResponse<TokenResponse>> refresh(@Valid RefreshTokenRequest request);

    @Operation(summary = "로그아웃", description = "전달한 리프레시 토큰을 폐기합니다. 액세스 토큰 자체는 만료 전까지 유효하지만, "
            + "재발급에는 더 이상 사용할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요함 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/auth/logout\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}")))
    })
    ResponseEntity<DataResponse<Void>> logout(@Valid RefreshTokenRequest request);
}
