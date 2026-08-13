package com.nexters.palang.domain.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AppleLoginRequest(
        @NotBlank(message = "애플 identity token은 필수입니다.")
        @Schema(example = "apple-identity-token")
        String identityToken,

        @Schema(example = "apple-authorization-code",
                description = "회원탈퇴 시 애플 연동 해제(revoke)용 refresh token 확보에 사용한다. 없어도 로그인 자체는 진행된다.")
        String authorizationCode,

        @Schema(example = "길동", description = "애플이 최초 로그인 시에만 내려주는 이름. 이후 로그인엔 null일 수 있다.")
        String givenName,

        @Schema(example = "홍", description = "애플이 최초 로그인 시에만 내려주는 성. 이후 로그인엔 null일 수 있다.")
        String familyName
) {
}
