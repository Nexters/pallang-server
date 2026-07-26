package com.nexters.palang.domain.book.common.error;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookErrorCode implements BaseErrorCode {

    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK_404_1", "해당 도서를 찾을 수 없습니다."),
    EXTERNAL_SEARCH_FAILED(HttpStatus.BAD_REQUEST, "BOOK_400_1", "외부 도서 검색에 실패했습니다. 잠시 후 다시 시도해주세요."),
    INVALID_CURRENT_PAGE(HttpStatus.BAD_REQUEST, "BOOK_400_2", "현재 페이지는 도서의 전체 페이지 수를 초과할 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
