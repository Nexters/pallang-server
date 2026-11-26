package com.nexters.palang.domain.group.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

// 모임 카드에 보여줄 "지금 읽는 책" 요약(L02~L05).
// - readingBookCount: 조회 시점에 함께 읽는(READING) 책 수. 완독한 책까지 더한 전체 등록 권수가 아니다.
// - latestReadingEndDate: 읽는 책 중 종료일이 설정된 책들의 종료일 최댓값. 기간 없는 책은 계산에서 뺀다.
//   모임 자체의 종료일이나 기록/초대의 만료를 뜻하지 않는다(모임 종료일은 저장하지 않는다).
public record GroupReadingSummary(long readingBookCount, LocalDate latestReadingEndDate, ReadingScheduleStatus scheduleStatus) {

    public static GroupReadingSummary of(Collection<GroupBookEntry> entries, LocalDate today) {
        List<GroupBookEntry> readingEntries = entries.stream()
                .filter(entry -> entry.currentStatus(today) == ReadingStatus.READING)
                .toList();
        LocalDate latestEndDate = readingEntries.stream()
                .map(GroupBookEntry::getEndDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new GroupReadingSummary(readingEntries.size(), latestEndDate, scheduleStatus(readingEntries, latestEndDate));
    }

    private static ReadingScheduleStatus scheduleStatus(List<GroupBookEntry> readingEntries, LocalDate latestEndDate) {
        if (readingEntries.isEmpty()) {
            return ReadingScheduleStatus.ALL_FINISHED;
        }
        return latestEndDate != null ? ReadingScheduleStatus.HAS_END_DATE : ReadingScheduleStatus.NO_PERIOD;
    }
}
