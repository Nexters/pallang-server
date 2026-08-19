package com.nexters.palang.domain.passage.common.error;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PassageErrorCode implements BaseErrorCode {

    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "PASSAGE_400_1", "이미지 파일만 업로드할 수 있습니다."),
    PASSAGE_BOOK_MISMATCH(HttpStatus.BAD_REQUEST, "PASSAGE_400_2", "선택한 대목이 지정한 도서와 일치하지 않습니다."),
    PASSAGE_GROUP_MISMATCH(HttpStatus.BAD_REQUEST, "PASSAGE_400_3", "선택한 대목이 지정한 모임과 일치하지 않습니다."),
    PASSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "PASSAGE_404_1", "해당 대목을 찾을 수 없습니다."),
    PASSAGE_FORBIDDEN(HttpStatus.FORBIDDEN, "PASSAGE_403_1", "본인이 흔적을 남긴 대목만 스포일러 설정을 변경할 수 있습니다."),
    OCR_TEXT_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "PASSAGE_422_1", "이미지에서 텍스트를 인식하지 못했습니다."),
    OCR_REQUEST_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "PASSAGE_503_1", "OCR 처리 중 오류가 발생했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
