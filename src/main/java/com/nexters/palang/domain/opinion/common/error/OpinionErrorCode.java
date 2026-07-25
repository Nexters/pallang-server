package com.nexters.palang.domain.opinion.common.error;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OpinionErrorCode implements BaseErrorCode {

    OPINION_NOT_FOUND(HttpStatus.NOT_FOUND, "OPINION_404_1", "해당 흔적을 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
