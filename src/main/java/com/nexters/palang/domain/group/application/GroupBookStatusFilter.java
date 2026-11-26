package com.nexters.palang.domain.group.application;

import com.nexters.palang.domain.group.domain.ReadingStatus;

// 모임 책 목록 조회 범위. 책 목록 화면의 탭은 READING("함께 읽는 책") / FINISHED("완독한 책")이고, 모임 설정
// 화면의 등록 책 목록은 ALL이다.
public enum GroupBookStatusFilter {
    READING,
    FINISHED,
    ALL;

    public ReadingStatus toReadingStatus() {
        return switch (this) {
            case READING -> ReadingStatus.READING;
            case FINISHED -> ReadingStatus.FINISHED;
            case ALL -> null;
        };
    }
}
