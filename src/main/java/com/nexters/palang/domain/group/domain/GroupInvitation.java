package com.nexters.palang.domain.group.domain;

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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 모임 초대 링크. 예전에는 Group.invite_code 하나였지만, 발급 시각과 상태를 따로 남겨야
// "내보내진 계정은 removedAt 이전에 발급된 초대로 재가입 불가"(#226), "합치기 시 대상 모임의 초대 일괄 종료"(#227)
// 같은 정책을 판단할 수 있어 테이블로 분리했다. 발급 시각은 created_at(BaseEntity)이다.
// 별도 만료 시각은 두지 않는다 — 재발급(revoke 후 새로 발급)으로만 무효화한다.
@Getter
@Entity
@Table(
        name = "group_invitations",
        uniqueConstraints = @UniqueConstraint(name = "uq_group_invitations_code", columnNames = "code"),
        indexes = @Index(name = "idx_group_invitations_group_status", columnList = "group_id, status")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupInvitation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private Group group;

    @Column(name = "code", length = 32, nullable = false, updatable = false)
    private String code;

    // 발급한 모임장의 user id. 회원 삭제(AdminUserService)와 FK로 얽히지 않도록 연관관계 없이 값만 둔다.
    @Column(name = "issued_by", nullable = false, updatable = false)
    private Long issuedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private GroupInvitationStatus status;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    private GroupInvitation(Group group, String code, Long issuedBy) {
        this.group = group;
        this.code = code;
        this.issuedBy = issuedBy;
        this.status = GroupInvitationStatus.ACTIVE;
    }

    public static GroupInvitation issue(Group group, Long issuedBy) {
        return new GroupInvitation(group, UUID.randomUUID().toString().replace("-", ""), issuedBy);
    }

    public boolean isActive() {
        return status == GroupInvitationStatus.ACTIVE;
    }

    public void revoke(LocalDateTime now) {
        if (isActive()) {
            this.status = GroupInvitationStatus.REVOKED;
            this.revokedAt = now;
        }
    }
}
