package com.nexters.palang.domain.group.domain;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Getter
@Entity
// 테이블명을 "groups"로 두면 MySQL 8(GROUPS가 예약어, GROUPS BETWEEN 윈도우 프레임 구문 도입 이후)에서
// DDL/쿼리가 깨질 수 있어 "reading_groups"로 둔다.
@Table(
        name = "reading_groups",
        uniqueConstraints = @UniqueConstraint(name = "uq_reading_groups_invite_code", columnNames = "invite_code")
)
@Check(constraints = "capacity BETWEEN 2 AND 10 AND end_date >= start_date")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group extends BaseEntity {

    public static final int NAME_MAX_LENGTH = 15;
    public static final int MIN_CAPACITY = 2;
    public static final int MAX_CAPACITY = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "name", length = NAME_MAX_LENGTH, nullable = false)
    private String name;

    // 모임을 만들 때 함께 고른 책. 흔적/의견이 이 책을 기준으로 쌓이므로 생성 후에는 바꿀 수 없다(setter 없음).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false, updatable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false, updatable = false)
    private User host;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // 카카오톡 등으로 공유하는 초대 링크의 식별자. 별도 만료 시각을 두지 않으며, endDate가 지나도
    // 무효화되지 않는다 — 기간은 강제되지 않는 안내용 값이라는 결정(isEnded() 주석 참고)과 일관되게,
    // 종료된 모임에도 초대 링크로 새 멤버가 계속 가입할 수 있다. 유출 등으로 재발급이 필요하면
    // regenerateInviteCode()로 기존 코드를 무효화한다.
    @Column(name = "invite_code", length = 32, nullable = false)
    private String inviteCode;

    private Group(String name, Book book, User host, int capacity, LocalDate startDate, LocalDate endDate, String inviteCode) {
        this.name = name;
        this.book = book;
        this.host = host;
        this.capacity = capacity;
        this.startDate = startDate;
        this.endDate = endDate;
        this.inviteCode = inviteCode;
    }

    public static Group create(String name, Book book, User host, int capacity, LocalDate startDate, LocalDate endDate) {
        validateCapacity(capacity);
        validatePeriod(startDate, endDate);
        return new Group(name, book, host, capacity, startDate, endDate, generateInviteCode());
    }

    // 방 설정 변경(host 전용): 책은 대상이 아니다. 정원을 현재 참여 인원보다 적게 줄이는 것은 막는다.
    public void updateSettings(String name, int capacity, LocalDate startDate, LocalDate endDate, long currentMemberCount) {
        validateCapacity(capacity);
        validatePeriod(startDate, endDate);
        if (capacity < currentMemberCount) {
            throw new GroupException(GroupErrorCode.CAPACITY_BELOW_MEMBER_COUNT);
        }
        this.name = name;
        this.capacity = capacity;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // 기간은 모임원끼리 정하는 안내용 값일 뿐 강제되지 않는다(PM 확인: 기간이 지나도 모임 목록 노출,
    // 흔적/의견 작성 모두 계속 가능). isEnded()는 FE가 "종료됨" 배지를 보여주는 용도로만 쓰이며,
    // 어떤 조회/쓰기 API도 이 값으로 접근을 막지 않는다.
    public boolean isEnded() {
        return LocalDate.now().isAfter(endDate);
    }

    // 초대 링크 재발급(host 전용): 기존 코드는 즉시 무효화되고 이 코드로 들어온 이후 요청은 전부
    // INVALID_INVITE_CODE로 거절된다.
    public void regenerateInviteCode() {
        this.inviteCode = generateInviteCode();
    }

    private static void validateCapacity(int capacity) {
        if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
            throw new GroupException(GroupErrorCode.INVALID_GROUP_CAPACITY);
        }
    }

    private static void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new GroupException(GroupErrorCode.INVALID_GROUP_PERIOD);
        }
    }

    private static String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
