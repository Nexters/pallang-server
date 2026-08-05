package com.nexters.palang.domain.block.common;

import com.nexters.palang.global.common.error.AppException;

public class BlockException extends AppException {

    public BlockException(BlockErrorCode errorCode) {
        super(errorCode);
    }
}
