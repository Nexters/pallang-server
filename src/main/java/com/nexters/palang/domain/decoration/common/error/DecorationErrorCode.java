package com.nexters.palang.domain.decoration.common.error;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DecorationErrorCode implements BaseErrorCode {

    INVALID_RANGE(HttpStatus.BAD_REQUEST, "DECORATION_400_1", "꾸밈 효과의 시작 위치는 끝 위치보다 작아야 합니다."),
    OVERLAPPING_RANGE(HttpStatus.BAD_REQUEST, "DECORATION_400_2", "같은 흔적 안에서는 꾸밈 효과 영역이 겹칠 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
