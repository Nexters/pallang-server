package com.nexters.palang.domain.notification.domain;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "device_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uq_device_tokens_token", columnNames = "token"),
        indexes = @Index(name = "idx_device_tokens_user", columnList = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken extends BaseEntity {

    public static final int TOKEN_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", length = TOKEN_MAX_LENGTH, nullable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private DevicePlatform platform;

    @Column(name = "last_active_at", nullable = false)
    private LocalDateTime lastActiveAt;

    @Builder
    private DeviceToken(User user, String token, DevicePlatform platform) {
        this.user = user;
        this.token = token;
        this.platform = platform;
        this.lastActiveAt = LocalDateTime.now();
    }

    // 같은 기기에서 재로그인/재설치로 토큰이 재등록될 때: 소유자와 플랫폼을 최신 상태로 교체한다.
    public void reassign(User user, DevicePlatform platform) {
        this.user = user;
        this.platform = platform;
        this.lastActiveAt = LocalDateTime.now();
    }
}
