package com.nexters.palang.global.common.error;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {

    HttpStatus getHttpStatus();

    String getCustomCode();

    String getMessage();
}
