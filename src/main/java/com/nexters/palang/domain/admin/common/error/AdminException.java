package com.nexters.palang.domain.admin.common.error;

import com.nexters.palang.global.common.error.AppException;

public class AdminException extends AppException {

    public AdminException(AdminErrorCode errorCode) {
        super(errorCode);
    }

    public AdminException(AdminErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
