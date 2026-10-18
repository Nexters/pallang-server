package com.nexters.palang.domain.notification.domain;

import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 유저당 row가 없으면 "전체 허용"(활동/모임 모두 true)으로 취급한다. PUT 호출 전까지는 row를 만들지 않는다.
@Getter
@Entity
@Table(
        name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(name = "uq_notification_preferences_user", columnNames = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "activity_enabled", nullable = false)
    private boolean activityEnabled;

    @Column(name = "group_enabled", nullable = false)
    private boolean groupEnabled;

    @Builder
    private NotificationPreference(User user, boolean activityEnabled, boolean groupEnabled) {
        this.user = user;
        this.activityEnabled = activityEnabled;
        this.groupEnabled = groupEnabled;
    }

    public void update(boolean activityEnabled, boolean groupEnabled) {
        this.activityEnabled = activityEnabled;
        this.groupEnabled = groupEnabled;
    }
}
