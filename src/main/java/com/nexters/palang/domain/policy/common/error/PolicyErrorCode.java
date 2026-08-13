package com.nexters.palang.domain.policy.common.error;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PolicyErrorCode implements BaseErrorCode {

    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "POLICY_404_1", "해당 약관을 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
