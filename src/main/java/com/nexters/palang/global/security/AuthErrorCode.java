package com.nexters.palang.global.security;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "로그인이 필요합니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
