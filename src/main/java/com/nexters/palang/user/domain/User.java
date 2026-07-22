package com.nexters.palang.user.domain;

import com.nexters.palang.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_nickname", columnNames = "nickname"),
                @UniqueConstraint(name = "uq_users_sns", columnNames = {"sns_provider", "sns_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @Column(name = "nickname", length = 15, nullable = false)
    private String nickname;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(name = "background_color", length = 20)
    private String backgroundColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "sns_provider", nullable = false)
    private SnsProvider snsProvider;

    @Column(name = "sns_id", nullable = false)
    private String snsId;

    @Column(name = "terms_agreed_at", nullable = false)
    private LocalDateTime termsAgreedAt;

    @Column(name = "has_completed_onboarding", nullable = false)
    private boolean hasCompletedOnboarding;

    @Column(name = "is_withdrawn", nullable = false)
    private boolean isWithdrawn;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Builder
    private User(
            String nickname,
            String profileImageUrl,
            String backgroundColor,
            SnsProvider snsProvider,
            String snsId,
            LocalDateTime termsAgreedAt
    ) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.backgroundColor = backgroundColor;
        this.snsProvider = snsProvider;
        this.snsId = snsId;
        this.termsAgreedAt = termsAgreedAt;
        this.hasCompletedOnboarding = false;
        this.isWithdrawn = false;
    }

    public void completeOnboarding() {
        this.hasCompletedOnboarding = true;
    }

    public void withdraw() {
        this.isWithdrawn = true;
        this.withdrawnAt = LocalDateTime.now();
    }
}
