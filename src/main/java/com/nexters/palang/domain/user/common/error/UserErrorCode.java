package com.nexters.palang.domain.user.common.error;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_1", "해당 사용자를 찾을 수 없습니다."),
    NICKNAME_CHANGE_LIMITED(HttpStatus.BAD_REQUEST, "USER_400_1", "닉네임은 하루에 한 번만 변경할 수 있습니다."),
    NICKNAME_ALREADY_IN_USE(HttpStatus.CONFLICT, "USER_409_1", "이미 사용 중인 닉네임입니다."),
    NICKNAME_GENERATION_FAILED(HttpStatus.CONFLICT, "USER_409_2", "닉네임 자동 생성에 실패했습니다. 잠시 후 다시 시도해주세요."),
    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "USER_400_2", "이미지 파일만 업로드할 수 있습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
