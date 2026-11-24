package com.nexters.palang.domain.group.domain;

// AUTO: 종료일 기준으로 조회 시점에 상태를 계산한다(종료일 다음 날 00:00 KST부터 완독).
// MANUAL: 모임장이 직접 고른 상태(manualStatus)가 날짜보다 우선한다. AUTO로 되돌리는 기능은 없다.
public enum ReadingStatusMode {
    AUTO,
    MANUAL,
}
