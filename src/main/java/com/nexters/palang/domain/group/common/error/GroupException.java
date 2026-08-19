package com.nexters.palang.domain.group.common.error;

import com.nexters.palang.global.common.error.AppException;

public class GroupException extends AppException {

    public GroupException(GroupErrorCode errorCode) {
        super(errorCode);
    }
}
