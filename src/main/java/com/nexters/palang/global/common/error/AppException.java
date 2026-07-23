package com.nexters.palang.global.common.error;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public AppException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
