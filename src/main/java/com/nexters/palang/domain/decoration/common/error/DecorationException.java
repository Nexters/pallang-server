package com.nexters.palang.domain.decoration.common.error;

import com.nexters.palang.global.common.error.AppException;

public class DecorationException extends AppException {

    public DecorationException(DecorationErrorCode errorCode) {
        super(errorCode);
    }
}
