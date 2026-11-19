package com.nexters.palang.domain.group.domain;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

// 모임이 함께 읽는 책 한 권(모임 책). 판본(book)은 등록 후 바꿀 수 없고, 기간과 독서 상태만 수정한다.
// 같은 판본 중복 추가 차단은 서비스에서 한다 — 합치기(#227)로 같은 판본의 독립 entry가 한 모임에 여러 개
// 생길 수 있어 DB 유니크 제약은 두지 않는다. created_at(BaseEntity)이 책 목록 정렬 기준인 "등록 시각"이다.
@Getter
@Entity
@Table(
        name = "group_book_entries",
        indexes = {
                @Index(name = "idx_group_book_entries_group", columnList = "group_id, created_at"),
                @Index(name = "idx_group_book_entries_book", columnList = "book_id"),
                @Index(name = "idx_group_book_entries_end_date", columnList = "end_date")
        }
)
@Check(constraints = "(start_date IS NULL AND end_date IS NULL) "
        + "OR (start_date IS NOT NULL AND end_date IS NOT NULL AND start_date <= end_date)")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupBookEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false, updatable = false)
    private Book book;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_mode", length = 20, nullable = false)
    private ReadingStatusMode statusMode;

    // MANUAL일 때만 값이 있다.
    @Enumerated(EnumType.STRING)
    @Column(name = "manual_status", length = 20)
    private ReadingStatus manualStatus;

    private GroupBookEntry(Group group, Book book, ReadingPeriod period) {
        this.group = group;
        this.book = book;
        this.startDate = period.startDate();
        this.endDate = period.endDate();
        this.statusMode = ReadingStatusMode.AUTO;
    }

    // 새로 등록하는 기간의 날짜는 모두 오늘 이후여야 한다.
    public static GroupBookEntry create(Group group, Book book, ReadingPeriod period, LocalDate today) {
        period.validateChangedDatesNotPast(ReadingPeriod.NONE, today);
        return new GroupBookEntry(group, book, period);
    }

    public ReadingPeriod getPeriod() {
        return ReadingPeriod.of(startDate, endDate);
    }

    // 기간만 바꾼다. 바꾸지 않은 기존 과거 날짜는 그대로 허용하고, 상태 모드(AUTO/MANUAL)는 유지한다.
    public void changePeriod(ReadingPeriod newPeriod, LocalDate today) {
        newPeriod.validateChangedDatesNotPast(getPeriod(), today);
        this.startDate = newPeriod.startDate();
        this.endDate = newPeriod.endDate();
    }

    // 관리자 데이터 정정(AdminGroupService) 전용: "새 날짜는 오늘 이후" 규칙 없이 기간을 그대로 덮어쓴다.
    public void correctPeriod(ReadingPeriod period) {
        this.startDate = period.startDate();
        this.endDate = period.endDate();
    }

    // 모임장이 상태를 직접 고른 경우에만 호출한다. 이후로는 날짜와 무관하게 이 값이 유지된다.
    public void changeStatusManually(ReadingStatus status) {
        this.statusMode = ReadingStatusMode.MANUAL;
        this.manualStatus = status;
    }

    // 조회 시점 기준 상태. AUTO는 종료일 당일 23:59:59(KST)까지 읽는 중, 다음 날 00:00부터 완독이다.
    // 배치로 상태를 저장해두지 않고 매번 계산해야 배치 지연으로 옛 탭/권수가 보이지 않는다.
    public ReadingStatus currentStatus(LocalDate today) {
        if (statusMode == ReadingStatusMode.MANUAL) {
            return manualStatus;
        }
        if (endDate != null && today.isAfter(endDate)) {
            return ReadingStatus.FINISHED;
        }
        return ReadingStatus.READING;
    }
}
