package com.nexters.palang.global.common.error;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final BaseErrorCode errorCode;
    private final String detail;

    public AppException(BaseErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    // errorCode.getMessage()는 고정 문구라, 어떤 모임/대목 때문에 막혔는지처럼 요청마다 달라지는
    // 안내가 필요한 경우(AdminException 등) 이 생성자로 detail을 따로 넘긴다.
    public AppException(BaseErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
        this.detail = detail;
    }
}
