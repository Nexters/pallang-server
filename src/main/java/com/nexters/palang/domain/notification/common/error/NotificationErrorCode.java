package com.nexters.palang.domain.notification.common.error;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_404_1", "해당 알림을 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
