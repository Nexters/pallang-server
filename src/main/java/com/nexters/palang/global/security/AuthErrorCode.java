package com.nexters.palang.global.security;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "로그인이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_401_2", "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_3", "유효하지 않은 토큰입니다."),
    KAKAO_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_401_4", "카카오 인증에 실패했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_5", "유효하지 않은 리프레시 토큰입니다."),
    APPLE_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_401_6", "애플 인증에 실패했습니다."),
    WITHDRAWN_ACCOUNT(HttpStatus.FORBIDDEN, "AUTH_403_1", "탈퇴한 계정입니다."),
    KAKAO_UNLINK_FAILED(HttpStatus.BAD_GATEWAY, "AUTH_502_1", "카카오 연동 해제에 실패했습니다."),
    APPLE_REVOKE_FAILED(HttpStatus.BAD_GATEWAY, "AUTH_502_2", "애플 연동 해제에 실패했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
