package com.nexters.palang.domain.book.common;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookErrorCode implements BaseErrorCode {

    EXTERNAL_SEARCH_FAILED(HttpStatus.BAD_REQUEST, "BOOK_400_1", "외부 도서 검색에 실패했습니다. 잠시 후 다시 시도해주세요."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
