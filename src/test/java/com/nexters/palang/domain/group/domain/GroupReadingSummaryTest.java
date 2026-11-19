package com.nexters.palang.domain.group.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.user.domain.User;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GroupReadingSummaryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    private final Group group = group();

    private Group group() {
        User host = User.builder().nickname("호스트").build();
        ReflectionTestUtils.setField(host, "id", 1L);
        return Group.create("고전 뽀개기", host, 4);
    }

    private GroupBookEntry entry(ReadingPeriod period) {
        Book book = Book.builder().title("책").author("작가").publisher("출판사").pageCount(100).source(BookSource.API).build();
        return GroupBookEntry.create(group, book, period, LocalDate.MIN);
    }

    @Test
    @DisplayName("읽는 책 수는 완독한 책을 빼고 세며, 날짜는 읽는 책 중 가장 늦은 종료일이다")
    void countsReadingBooksAndLatestEndDate() {
        List<GroupBookEntry> entries = List.of(
                entry(ReadingPeriod.of(TODAY, TODAY.plusDays(10))),
                entry(ReadingPeriod.of(TODAY, TODAY.plusDays(30))),
                entry(ReadingPeriod.NONE),
                entry(ReadingPeriod.of(TODAY.minusDays(30), TODAY.minusDays(1))));

        GroupReadingSummary summary = GroupReadingSummary.of(entries, TODAY);

        assertThat(summary.readingBookCount()).isEqualTo(3);
        assertThat(summary.latestReadingEndDate()).isEqualTo(TODAY.plusDays(30));
        assertThat(summary.scheduleStatus()).isEqualTo(ReadingScheduleStatus.HAS_END_DATE);
    }

    @Test
    @DisplayName("완독한 책의 종료일은 더 늦어도 카드 날짜에 쓰지 않는다")
    void ignoresFinishedBooksEndDate() {
        GroupBookEntry finishedLater = entry(ReadingPeriod.of(TODAY, TODAY.plusDays(60)));
        finishedLater.changeStatusManually(ReadingStatus.FINISHED);

        GroupReadingSummary summary = GroupReadingSummary.of(
                List.of(entry(ReadingPeriod.of(TODAY, TODAY.plusDays(10))), finishedLater), TODAY);

        assertThat(summary.latestReadingEndDate()).isEqualTo(TODAY.plusDays(10));
    }

    @Test
    @DisplayName("읽는 책이 모두 기간이 없으면 '기간 미설정'이다")
    void noPeriodWhenAllReadingBooksHaveNoPeriod() {
        GroupReadingSummary summary = GroupReadingSummary.of(
                List.of(entry(ReadingPeriod.NONE), entry(ReadingPeriod.NONE)), TODAY);

        assertThat(summary.readingBookCount()).isEqualTo(2);
        assertThat(summary.latestReadingEndDate()).isNull();
        assertThat(summary.scheduleStatus()).isEqualTo(ReadingScheduleStatus.NO_PERIOD);
    }

    @Test
    @DisplayName("읽는 책이 0권이면 '모든 책 완독'이다")
    void allFinishedWhenNoReadingBooks() {
        GroupReadingSummary summary = GroupReadingSummary.of(
                List.of(entry(ReadingPeriod.of(TODAY.minusDays(10), TODAY.minusDays(1)))), TODAY);

        assertThat(summary.readingBookCount()).isZero();
        assertThat(summary.latestReadingEndDate()).isNull();
        assertThat(summary.scheduleStatus()).isEqualTo(ReadingScheduleStatus.ALL_FINISHED);
    }
}
