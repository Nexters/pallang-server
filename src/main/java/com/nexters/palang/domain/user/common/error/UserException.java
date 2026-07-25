package com.nexters.palang.domain.user.common.error;

import com.nexters.palang.global.common.error.AppException;

public class UserException extends AppException {

    public UserException(UserErrorCode errorCode) {
        super(errorCode);
    }
}
