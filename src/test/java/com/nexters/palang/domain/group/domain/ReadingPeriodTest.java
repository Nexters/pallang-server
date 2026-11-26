package com.nexters.palang.domain.group.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReadingPeriodTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);

    @Test
    @DisplayName("시작일과 종료일을 모두 비우면 기간 없음이다")
    void bothEmptyMeansNoPeriod() {
        assertThat(ReadingPeriod.of(null, null).isEmpty()).isTrue();
        assertThat(ReadingPeriod.of(null, null)).isEqualTo(ReadingPeriod.NONE);
    }

    @Test
    @DisplayName("시작일과 종료일이 같은 하루짜리 기간은 허용한다")
    void allowsSingleDayPeriod() {
        assertThatCode(() -> ReadingPeriod.of(TODAY, TODAY)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("시작일이나 종료일 한쪽만 있으면 예외가 발생한다")
    void rejectsHalfPeriod() {
        assertThatThrownBy(() -> ReadingPeriod.of(TODAY, null))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.INCOMPLETE_GROUP_PERIOD);
        assertThatThrownBy(() -> ReadingPeriod.of(null, TODAY))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.INCOMPLETE_GROUP_PERIOD);
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 예외가 발생한다")
    void rejectsReversedPeriod() {
        assertThatThrownBy(() -> ReadingPeriod.of(TODAY.plusDays(1), TODAY))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.INVALID_GROUP_PERIOD);
    }

    @Test
    @DisplayName("새로 입력하는 기간에 오늘 이전 날짜가 있으면 예외가 발생한다")
    void rejectsNewPastDate() {
        ReadingPeriod period = ReadingPeriod.of(TODAY.minusDays(1), TODAY.plusDays(5));

        assertThatThrownBy(() -> period.validateChangedDatesNotPast(ReadingPeriod.NONE, TODAY))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.PAST_GROUP_DATE);
    }

    @Test
    @DisplayName("오늘 날짜는 새로 입력할 수 있다")
    void allowsToday() {
        assertThatCode(() -> ReadingPeriod.of(TODAY, TODAY).validateChangedDatesNotPast(ReadingPeriod.NONE, TODAY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("과거 시작일을 바꾸지 않고 종료일만 미래로 연장하는 것은 허용한다")
    void allowsExtendingEndDateWhileKeepingPastStartDate() {
        ReadingPeriod previous = ReadingPeriod.of(TODAY.minusDays(30), TODAY.minusDays(1));
        ReadingPeriod extended = ReadingPeriod.of(TODAY.minusDays(30), TODAY.plusDays(10));

        assertThatCode(() -> extended.validateChangedDatesNotPast(previous, TODAY)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("바꾸지 않은 기존 과거 기간은 그대로 다시 저장할 수 있다")
    void allowsUnchangedPastPeriod() {
        ReadingPeriod previous = ReadingPeriod.of(TODAY.minusDays(30), TODAY.minusDays(1));

        assertThatCode(() -> previous.validateChangedDatesNotPast(previous, TODAY)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("과거 날짜를 다른 과거 날짜로 바꾸면 예외가 발생한다")
    void rejectsChangingToAnotherPastDate() {
        ReadingPeriod previous = ReadingPeriod.of(TODAY.minusDays(30), TODAY.minusDays(1));
        ReadingPeriod changed = ReadingPeriod.of(TODAY.minusDays(20), TODAY.minusDays(1));

        assertThatThrownBy(() -> changed.validateChangedDatesNotPast(previous, TODAY))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.PAST_GROUP_DATE);
    }

    @Test
    @DisplayName("기간을 양쪽 모두 지우는 것은 허용한다")
    void allowsClearingPeriod() {
        ReadingPeriod previous = ReadingPeriod.of(TODAY.minusDays(30), TODAY.minusDays(1));

        assertThatCode(() -> ReadingPeriod.NONE.validateChangedDatesNotPast(previous, TODAY)).doesNotThrowAnyException();
    }
}
