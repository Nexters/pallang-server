package com.nexters.palang.domain.comment.common;

import com.nexters.palang.global.common.error.AppException;

public class CommentException extends AppException {

    public CommentException(CommentErrorCode errorCode) {
        super(errorCode);
    }
}
