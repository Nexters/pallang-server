package com.nexters.palang.domain.block.common;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BlockErrorCode implements BaseErrorCode {

    SELF_BLOCK_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "BLOCK_400_1", "본인을 차단할 수 없습니다."),
    ALREADY_BLOCKED(HttpStatus.CONFLICT, "BLOCK_409_1", "이미 차단한 사용자입니다."),
    BLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "BLOCK_404_1", "차단 내역을 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
