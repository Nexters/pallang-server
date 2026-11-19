package com.nexters.palang.domain.group.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.user.domain.User;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GroupBookEntryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);

    private Group group() {
        User host = User.builder().nickname("호스트").build();
        ReflectionTestUtils.setField(host, "id", 1L);
        return Group.create("고전 뽀개기", host, 4);
    }

    private Book book() {
        return Book.builder()
                .title("채식주의자").author("한강").publisher("창비").pageCount(268).source(BookSource.API).build();
    }

    private GroupBookEntry entry(ReadingPeriod period) {
        return GroupBookEntry.create(group(), book(), period, TODAY);
    }

    @Test
    @DisplayName("기간 없이 책을 등록할 수 있고, 날짜 기준 자동 판정(AUTO) 상태로 시작한다")
    void createsEntryWithoutPeriod() {
        GroupBookEntry entry = entry(ReadingPeriod.NONE);

        assertThat(entry.getPeriod().isEmpty()).isTrue();
        assertThat(entry.getStatusMode()).isEqualTo(ReadingStatusMode.AUTO);
        assertThat(entry.currentStatus(TODAY)).isEqualTo(ReadingStatus.READING);
    }

    @Test
    @DisplayName("새로 등록하는 기간에 과거 날짜가 있으면 예외가 발생한다")
    void createFailsWithPastDate() {
        assertThatThrownBy(() -> entry(ReadingPeriod.of(TODAY.minusDays(1), TODAY.plusDays(1))))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.PAST_GROUP_DATE);
    }

    @Test
    @DisplayName("자동 판정이면 종료일 당일까지는 읽는 중이고 다음 날부터 완독이다")
    void autoStatusFollowsEndDate() {
        GroupBookEntry entry = entry(ReadingPeriod.of(TODAY, TODAY.plusDays(3)));

        assertThat(entry.currentStatus(TODAY.plusDays(3))).isEqualTo(ReadingStatus.READING);
        assertThat(entry.currentStatus(TODAY.plusDays(4))).isEqualTo(ReadingStatus.FINISHED);
    }

    @Test
    @DisplayName("모임장이 직접 완독을 고르면 종료일이 남아 있어도 완독이다")
    void manualFinishedOverridesDate() {
        GroupBookEntry entry = entry(ReadingPeriod.of(TODAY, TODAY.plusDays(30)));

        entry.changeStatusManually(ReadingStatus.FINISHED);

        assertThat(entry.getStatusMode()).isEqualTo(ReadingStatusMode.MANUAL);
        assertThat(entry.currentStatus(TODAY)).isEqualTo(ReadingStatus.FINISHED);
    }

    @Test
    @DisplayName("모임장이 직접 읽는 중을 고르면 종료일이 지나도 읽는 중이다")
    void manualReadingOverridesDate() {
        GroupBookEntry entry = entry(ReadingPeriod.of(TODAY, TODAY));

        entry.changeStatusManually(ReadingStatus.READING);

        assertThat(entry.currentStatus(TODAY.plusDays(10))).isEqualTo(ReadingStatus.READING);
    }

    @Test
    @DisplayName("날짜만 바꾸면 기존 상태 모드는 유지된다")
    void changingPeriodKeepsStatusMode() {
        GroupBookEntry entry = entry(ReadingPeriod.of(TODAY, TODAY.plusDays(3)));
        entry.changeStatusManually(ReadingStatus.FINISHED);

        entry.changePeriod(ReadingPeriod.of(TODAY, TODAY.plusDays(10)), TODAY);

        assertThat(entry.getStatusMode()).isEqualTo(ReadingStatusMode.MANUAL);
        assertThat(entry.getEndDate()).isEqualTo(TODAY.plusDays(10));
    }

    @Test
    @DisplayName("이미 지난 시작일은 두고 종료일만 연장할 수 있다")
    void canExtendEndDateKeepingPastStartDate() {
        GroupBookEntry entry = entry(ReadingPeriod.of(TODAY, TODAY.plusDays(3)));
        LocalDate later = TODAY.plusDays(10);

        entry.changePeriod(ReadingPeriod.of(TODAY, later.plusDays(5)), later);

        assertThat(entry.getStartDate()).isEqualTo(TODAY);
        assertThat(entry.getEndDate()).isEqualTo(later.plusDays(5));
    }

    @Test
    @DisplayName("기간을 바꿀 때 새 날짜가 과거면 예외가 발생하고 기존 기간은 그대로다")
    void changePeriodFailsWithNewPastDate() {
        GroupBookEntry entry = entry(ReadingPeriod.of(TODAY, TODAY.plusDays(3)));

        assertThatThrownBy(() -> entry.changePeriod(ReadingPeriod.of(TODAY.minusDays(1), TODAY.plusDays(3)), TODAY))
                .isInstanceOf(GroupException.class);

        assertThat(entry.getStartDate()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("관리자 정정은 과거 날짜로도 기간을 덮어쓴다")
    void correctPeriodAllowsPastDates() {
        GroupBookEntry entry = entry(ReadingPeriod.NONE);
        ReadingPeriod past = ReadingPeriod.of(TODAY.minusDays(30), TODAY.minusDays(1));

        entry.correctPeriod(past);

        assertThat(entry.getPeriod()).isEqualTo(past);
    }
}
