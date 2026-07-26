package com.nexters.palang.domain.opinion.common.error;

import com.nexters.palang.global.common.error.AppException;

public class OpinionException extends AppException {

    public OpinionException(OpinionErrorCode errorCode) {
        super(errorCode);
    }
}
