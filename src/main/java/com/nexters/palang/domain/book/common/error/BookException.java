package com.nexters.palang.domain.book.common.error;

import com.nexters.palang.global.common.error.AppException;

public class BookException extends AppException {

    public BookException(BookErrorCode errorCode) {
        super(errorCode);
    }

    public BookException(BookErrorCode errorCode, Throwable cause) {
        super(errorCode);
        initCause(cause);
    }
}
