package com.nexters.palang.domain.group.domain;

import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 멤버 내보내기 감사 이력(5.2). 누가(actor) 누구를(target) 어떤 사유로 언제 내보냈는지 남긴다. 일반 멤버/대상에게는
// 공개하지 않고 운영 목적으로만 본다.
// - 사유 코드는 enum 이름을 문자열로 저장한다. @Enumerated로 매핑하면 Hibernate가 MySQL ENUM이나 값 목록 CHECK 제약을
//   만들어, 사유가 늘 때마다 운영 DB를 수동 ALTER해야 한다(Notification.removalReason과 같은 이유).
// - 회원/모임 삭제와 FK로 얽히지 않도록 연관관계 없이 id 값만 둔다.
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "group_member_removals",
        indexes = {
                @Index(name = "idx_group_member_removals_group", columnList = "group_id"),
                @Index(name = "idx_group_member_removals_target", columnList = "target_user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMemberRemoval {

    public static final int OTHER_TEXT_MAX_GRAPHEME_LENGTH = 200;
    // 200 grapheme이라도 이모지 등은 코드 포인트가 훨씬 많을 수 있어 컬럼은 여유 있게 둔다.
    public static final int OTHER_TEXT_COLUMN_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "group_id", nullable = false, updatable = false)
    private Long groupId;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private Long actorUserId;

    @Column(name = "target_user_id", nullable = false, updatable = false)
    private Long targetUserId;

    @Column(name = "reason_code", length = 40, nullable = false, updatable = false)
    private String reasonCode;

    @Column(name = "other_text", length = OTHER_TEXT_COLUMN_LENGTH, updatable = false)
    private String otherText;

    @Column(name = "request_key", length = 100, updatable = false)
    private String requestKey;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private GroupMemberRemoval(
            Long groupId, Long actorUserId, Long targetUserId, GroupMemberRemovalReason reason, String otherText,
            String requestKey) {
        this.groupId = groupId;
        this.actorUserId = actorUserId;
        this.targetUserId = targetUserId;
        this.reasonCode = reason.name();
        this.otherText = otherText;
        this.requestKey = requestKey;
    }

    // 사유는 1개 필수. OTHER면 직접 입력이 공백 제외 1~200자(grapheme)여야 하고, OTHER가 아니면 직접 입력을 받지 않는다.
    public static GroupMemberRemoval record(
            Long groupId, Long actorUserId, Long targetUserId, GroupMemberRemovalReason reason, String otherText,
            String requestKey) {
        return new GroupMemberRemoval(
                groupId, actorUserId, targetUserId, reason, normalizeOtherText(reason, otherText), requestKey);
    }

    public GroupMemberRemovalReason getReason() {
        return GroupMemberRemovalReason.valueOf(reasonCode);
    }

    static String normalizeOtherText(GroupMemberRemovalReason reason, String otherText) {
        if (reason == null) {
            throw new GroupException(GroupErrorCode.INVALID_REMOVAL_REASON);
        }
        String stripped = otherText != null ? otherText.strip() : "";
        if (reason != GroupMemberRemovalReason.OTHER) {
            if (!stripped.isEmpty()) {
                throw new GroupException(GroupErrorCode.INVALID_REMOVAL_REASON);
            }
            return null;
        }
        long length = GroupName.graphemeLength(stripped);
        if (length < 1 || length > OTHER_TEXT_MAX_GRAPHEME_LENGTH || stripped.length() > OTHER_TEXT_COLUMN_LENGTH) {
            throw new GroupException(GroupErrorCode.INVALID_REMOVAL_REASON);
        }
        return stripped;
    }
}
