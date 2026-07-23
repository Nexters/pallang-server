package com.nexters.palang.global.common.error;

import com.nexters.palang.global.common.response.BaseResponse;
import lombok.Getter;

@Getter
public class ErrorResponse extends BaseResponse {

    private final String type;
    private final String title;
    private final int status;
    private final String detail;

    private ErrorResponse(String type, String title, int status, String detail) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
    }

    public static ErrorResponse of(String requestUri, BaseErrorCode errorCode) {
        return of(requestUri, errorCode, errorCode.getMessage());
    }

    public static ErrorResponse of(String requestUri, BaseErrorCode errorCode, String detail) {
        return new ErrorResponse(requestUri, errorCode.getCustomCode(), errorCode.getHttpStatus().value(), detail);
    }
}
