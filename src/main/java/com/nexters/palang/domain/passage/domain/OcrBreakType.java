package com.nexters.palang.domain.passage.domain;

// OCR로 인식한 단어 뒤에 오는 끊김의 종류. 인쇄된 줄 끝(lineBreak)이 "지면 폭 때문에 꺾인 줄"인지
// "원문에서 실제로 끊는 줄(시의 행, 문단 끝)"인지 프론트가 구분할 수 있도록 내려준다.
// (Google Cloud Vision의 detectedBreak - EOL_SURE_SPACE / LINE_BREAK 와 같은 개념)
public enum OcrBreakType {
    // 줄 중간의 단어. 다음 단어와 공백으로 잇는다.
    NONE,
    // 지면 폭 때문에 꺾인 줄 끝. 다음 줄과 공백으로 잇는다.
    WRAP,
    // 원문에서 끊는 줄 끝(시의 행, 짧게 끝난 줄). 줄바꿈 한 번.
    LINE,
    // 문단·연이 끝나는 줄 끝(빈 줄, 다음 줄 들여쓰기). 빈 줄 또는 줄바꿈.
    PARAGRAPH,
}
