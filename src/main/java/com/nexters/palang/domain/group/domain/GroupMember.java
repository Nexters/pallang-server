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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    private GroupMember(Group group, User user, GroupMemberRole role) {
        this.group = group;
        this.user = user;
        this.role = role;
    }

    // 나가기/강퇴는 이번 스코프에 없어 정원 검증 등은 가입 플로우(후속 이슈)에서 다룬다. 여기서는
    // 이미 확정된 (group, user, role) 조합으로 멤버 레코드를 만드는 역할만 한다.
    public static GroupMember of(Group group, User user, GroupMemberRole role) {
        return new GroupMember(group, user, role);
    }
}
