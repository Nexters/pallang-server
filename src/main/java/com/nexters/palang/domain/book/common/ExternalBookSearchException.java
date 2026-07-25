package com.nexters.palang.domain.book.common;

import com.nexters.palang.global.common.error.AppException;

public class ExternalBookSearchException extends AppException {

    public ExternalBookSearchException(Throwable cause) {
        super(BookErrorCode.EXTERNAL_SEARCH_FAILED);
        initCause(cause);
    }
}
