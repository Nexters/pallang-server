package com.nexters.palang.domain.notification.common.error;

import com.nexters.palang.global.common.error.AppException;

public class NotificationException extends AppException {

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode);
    }
}
