package com.nexters.palang.domain.comment.common;

import com.nexters.palang.global.common.error.AppException;

public class NestedReplyNotAllowedException extends AppException {

    public NestedReplyNotAllowedException() {
        super(CommentErrorCode.NESTED_REPLY_NOT_ALLOWED);
    }
}
