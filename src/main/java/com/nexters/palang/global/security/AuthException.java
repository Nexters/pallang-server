package com.nexters.palang.global.security;

import com.nexters.palang.global.common.error.AppException;

public class AuthException extends AppException {

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
