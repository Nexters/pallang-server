package com.nexters.palang.domain.report.common;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReportErrorCode implements BaseErrorCode {

    DETAIL_REQUIRED_FOR_ETC(HttpStatus.BAD_REQUEST, "REPORT_400_1", "기타 사유를 선택한 경우 상세 내용을 입력해야 합니다."),
    SELF_REPORT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "REPORT_400_2", "본인이 작성한 콘텐츠는 신고할 수 없습니다."),
    DUPLICATE_REPORT(HttpStatus.CONFLICT, "REPORT_409_1", "이미 신고한 대상입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
