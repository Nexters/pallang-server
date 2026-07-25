package com.nexters.palang.domain.book.common.error;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookErrorCode implements BaseErrorCode {

    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK_404_1", "해당 도서를 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
