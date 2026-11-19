package com.nexters.palang.domain.group.domain;

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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// (모임, 사용자)당 행 하나. 탈퇴/내보내기는 행을 지우지 않고 status로 남기며, 다시 가입하면 같은 행을
// ACTIVE로 되살린다(rejoin) — 그래서 (group_id, user_id) 유니크 제약을 그대로 유지할 수 있다.
@Getter
@Entity
@Table(
        name = "group_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_group_members_group_user", columnNames = {"group_id", "user_id"}),
        indexes = @Index(name = "idx_group_members_user", columnList = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, updatable = false)
    private GroupMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private GroupMemberStatus status;

    // 현재 멤버십이 시작된 시각(참여일). 재가입하면 그 시각으로 갱신된다 — created_at은 최초 가입 시각으로 남는다.
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    // 내보내진 시각. "removedAt 이전에 발급된 초대로는 재가입 불가"(#226) 판단에 쓴다.
    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    private GroupMember(Group group, User user, GroupMemberRole role, LocalDateTime joinedAt) {
        this.group = group;
        this.user = user;
        this.role = role;
        this.status = GroupMemberStatus.ACTIVE;
        this.joinedAt = joinedAt;
    }

    public static GroupMember join(Group group, User user, GroupMemberRole role, LocalDateTime joinedAt) {
        return new GroupMember(group, user, role, joinedAt);
    }

    public boolean isActive() {
        return status == GroupMemberStatus.ACTIVE;
    }

    // 탈퇴/내보내기 후 유효한 초대로 다시 들어온 경우. 재가입 가능 여부(정원, 초대 발급 시각)는 호출 측이 검증한다.
    public void rejoin(LocalDateTime joinedAt) {
        this.status = GroupMemberStatus.ACTIVE;
        this.joinedAt = joinedAt;
    }
}
