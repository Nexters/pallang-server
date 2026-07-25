package com.nexters.palang.domain.passage.common.error;

import com.nexters.palang.global.common.error.AppException;

public class PassageException extends AppException {

    public PassageException(PassageErrorCode errorCode) {
        super(errorCode);
    }
}
