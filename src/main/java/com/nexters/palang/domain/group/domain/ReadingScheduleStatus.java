package com.nexters.palang.domain.group.domain;

// 모임 카드의 날짜 칸에 무엇을 보여줄지(L04).
// HAS_END_DATE: 읽는 책 중 종료일이 설정된 책이 있다 → 가장 늦은 종료일을 표시("~까지")
// NO_PERIOD: 읽는 책이 있지만 모두 기간이 없다 → "기간 미설정"
// ALL_FINISHED: 읽는 책이 0권이다 → "모든 책 완독"
public enum ReadingScheduleStatus {
    HAS_END_DATE,
    NO_PERIOD,
    ALL_FINISHED,
}
