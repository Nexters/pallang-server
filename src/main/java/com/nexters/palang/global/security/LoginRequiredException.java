package com.nexters.palang.global.security;

import com.nexters.palang.global.common.error.AppException;

public class LoginRequiredException extends AppException {

    public LoginRequiredException() {
        super(AuthErrorCode.LOGIN_REQUIRED);
    }
}
