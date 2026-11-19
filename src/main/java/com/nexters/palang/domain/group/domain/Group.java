package com.nexters.palang.domain.group.domain;

import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 모임 자체(이름/정원/모임장/상태)만 담는다. 함께 읽는 책과 기간은 책마다 다를 수 있어 GroupBookEntry로
// 분리했다(#223) — 모임 1개 : 모임 책 N개.
@Getter
@Entity
// 테이블명을 "groups"로 두면 MySQL 8(GROUPS가 예약어, GROUPS BETWEEN 윈도우 프레임 구문 도입 이후)에서
// DDL/쿼리가 깨질 수 있어 "reading_groups"로 둔다.
@Table(name = "reading_groups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group extends BaseEntity {

    public static final int MIN_CAPACITY = 2;
    public static final int MAX_CAPACITY = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "name", length = GroupName.COLUMN_LENGTH, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false, updatable = false)
    private User host;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private GroupStatus status;

    // 합치기(#227)로 흡수된 source 모임이면 흡수한 keeper 모임 id, 작업 id, 완료 시각을 남긴다.
    @Column(name = "merged_into_id")
    private Long mergedIntoId;

    @Column(name = "merge_job_id")
    private Long mergeJobId;

    @Column(name = "merged_at")
    private LocalDateTime mergedAt;

    // 모임 설정 동시 수정 충돌 검사용(S03). 클라이언트가 받은 version과 다르면 저장을 거절한다.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    private Group(String name, User host, int capacity) {
        this.name = name;
        this.host = host;
        this.capacity = capacity;
        this.status = GroupStatus.ACTIVE;
    }

    public static Group create(String name, User host, int capacity) {
        validateCapacity(capacity);
        return new Group(GroupName.normalize(name), host, capacity);
    }

    // 모임 설정 변경(host 전용): 이름과 정원만 바꾼다. 정원을 현재 참여 인원보다 적게 줄이는 것은 막는다.
    // 검증이 하나라도 실패하면 아무 필드도 바꾸지 않는다(S01).
    public void updateSettings(String name, int capacity, long currentMemberCount) {
        String normalizedName = GroupName.normalize(name);
        validateCapacity(capacity);
        if (capacity < currentMemberCount) {
            throw new GroupException(GroupErrorCode.CAPACITY_BELOW_MEMBER_COUNT);
        }
        this.name = normalizedName;
        this.capacity = capacity;
    }

    public boolean isActive() {
        return status == GroupStatus.ACTIVE;
    }

    public boolean isHost(Long userId) {
        return host.getId().equals(userId);
    }

    private static void validateCapacity(int capacity) {
        if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
            throw new GroupException(GroupErrorCode.INVALID_GROUP_CAPACITY);
        }
    }
}
