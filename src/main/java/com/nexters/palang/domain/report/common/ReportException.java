package com.nexters.palang.domain.report.common;

import com.nexters.palang.global.common.error.AppException;

public class ReportException extends AppException {

    public ReportException(ReportErrorCode errorCode) {
        super(errorCode);
    }
}
