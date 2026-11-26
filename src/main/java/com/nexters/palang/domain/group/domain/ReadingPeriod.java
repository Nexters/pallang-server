package com.nexters.palang.domain.group.domain;

import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import java.time.LocalDate;
import java.util.Objects;

// 모임 책의 독서 기간. "둘 다 없음" 또는 "시작 ≤ 종료인 날짜 쌍"만 허용한다 — 한쪽만 있는 기간은 만들 수 없다.
public record ReadingPeriod(LocalDate startDate, LocalDate endDate) {

    public static final ReadingPeriod NONE = new ReadingPeriod(null, null);

    public ReadingPeriod {
        if ((startDate == null) != (endDate == null)) {
            throw new GroupException(GroupErrorCode.INCOMPLETE_GROUP_PERIOD);
        }
        if (startDate != null && startDate.isAfter(endDate)) {
            throw new GroupException(GroupErrorCode.INVALID_GROUP_PERIOD);
        }
    }

    public static ReadingPeriod of(LocalDate startDate, LocalDate endDate) {
        return new ReadingPeriod(startDate, endDate);
    }

    public boolean isEmpty() {
        return startDate == null;
    }

    // 새로 입력하거나 바꾼 날짜는 오늘(Asia/Seoul) 이후여야 한다. 단, 이전 값과 같은(=바꾸지 않은) 날짜는
    // 과거여도 그대로 둔다 — 과거 시작일은 두고 종료일만 연장하거나, 상태만 바꾸려는 저장을 막지 않기 위해서다.
    // 새로 만드는 경우에는 previous로 NONE을 넘긴다.
    public void validateChangedDatesNotPast(ReadingPeriod previous, LocalDate today) {
        if (isChangedPastDate(startDate, previous.startDate(), today)
                || isChangedPastDate(endDate, previous.endDate(), today)) {
            throw new GroupException(GroupErrorCode.PAST_GROUP_DATE);
        }
    }

    private static boolean isChangedPastDate(LocalDate value, LocalDate previousValue, LocalDate today) {
        return value != null && !Objects.equals(value, previousValue) && value.isBefore(today);
    }
}
