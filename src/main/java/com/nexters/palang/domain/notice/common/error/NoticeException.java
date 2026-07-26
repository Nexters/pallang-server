package com.nexters.palang.domain.notice.common.error;

import com.nexters.palang.global.common.error.AppException;

public class NoticeException extends AppException {

    public NoticeException(NoticeErrorCode errorCode) {
        super(errorCode);
    }
}
