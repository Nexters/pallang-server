package com.nexters.palang.domain.policy.common.error;

import com.nexters.palang.global.common.error.AppException;

public class PolicyException extends AppException {

    public PolicyException(PolicyErrorCode errorCode) {
        super(errorCode);
    }
}
