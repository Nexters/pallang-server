package com.nexters.palang.domain.admin.common.error;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminErrorCode implements BaseErrorCode {

    ADMIN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ADMIN_403_1", "관리자 권한이 없습니다."),
    ADMIN_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "ADMIN_401_1", "아이디 또는 비밀번호가 올바르지 않습니다."),
    USER_DELETE_BLOCKED_BY_HOSTED_GROUP(HttpStatus.CONFLICT, "ADMIN_409_1",
            "다른 멤버가 있는 모임의 호스트여서 삭제할 수 없습니다."),
    USER_DELETE_BLOCKED_BY_PASSAGE(HttpStatus.CONFLICT, "ADMIN_409_2",
            "다른 사용자의 의견이 달린 흔적을 작성해서 삭제할 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
