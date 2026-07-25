package com.nexters.palang.domain.comment.common;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommentErrorCode implements BaseErrorCode {

    NESTED_REPLY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "COMMENT_400_1", "답글에는 답글을 남길 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
