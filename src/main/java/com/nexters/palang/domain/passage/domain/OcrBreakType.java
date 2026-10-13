package com.nexters.palang.domain.passage.domain;

// OCR로 인식한 단어 뒤에 오는 끊김의 종류. 인쇄된 줄 끝(lineBreak)이 "지면 폭 때문에 꺾인 줄"인지
// "원문에서 실제로 끊는 줄(시의 행, 문단 끝)"인지 프론트가 구분할 수 있도록 내려준다.
// (Google Cloud Vision의 detectedBreak - EOL_SURE_SPACE / LINE_BREAK 와 같은 개념)
// 줄 시작의 들여쓰기는 끊김과 별개로 OcrResultDto.indented로 표시한다(사진 첫 줄은 앞 줄 끝이 없어 여기에 담을 수 없다).
public enum OcrBreakType {
    // 줄 중간의 단어. 다음 단어와 공백으로 잇는다.
    NONE,
    // 지면 폭 때문에 꺾인 줄 끝. 다음 줄과 공백으로 잇는다.
    WRAP,
    // 시(운문)의 행 끝. 줄바꿈 한 번.
    LINE,
    // 산문의 문단 끝. 책에는 빈 줄 없이 다음 줄 들여쓰기로만 표시된다. 줄바꿈 한 번(들여쓰기는 다음 줄 첫 단어의 indented).
    PARAGRAPH,
    // 다음 줄과의 사이에 실제로 빈 줄이 있는 줄 끝(시의 연 구분, 제목 아래, 장면 전환). 빈 줄 하나.
    BLANK_LINE,
}
