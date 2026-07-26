package com.nexters.palang.domain.user.domain;

import com.nexters.palang.domain.user.common.error.UserErrorCode;
import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.NoArgsConstructor;

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

    public static final int NICKNAME_MAX_LENGTH = 15;
    public static final int BACKGROUND_COLOR_MAX_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "nickname", length = NICKNAME_MAX_LENGTH, nullable = false)
    private String nickname;

    @Column(name = "nickname_updated_at")
    private LocalDateTime nicknameUpdatedAt;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(name = "background_color", length = BACKGROUND_COLOR_MAX_LENGTH)
    private String backgroundColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "sns_provider", nullable = false)
    private SnsProvider snsProvider;

    @Column(name = "sns_id", nullable = false)
    private String snsId;

    // SNS 로그인 시점에는 아직 약관에 동의하지 않았을 수 있어(FR-AUTH-03) nullable이다.
    // POST /api/auth/terms 호출 시 agreeToTerms()로 채워진다.
    @Column(name = "terms_agreed_at")
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

    // 이용약관 동의(FR-AUTH-03): 재동의 요청이 와도 그냥 최신 시각으로 갱신한다(멱등).
    public void agreeToTerms() {
        this.termsAgreedAt = LocalDateTime.now();
    }

    // 하루 1회 제한(FR-MY-05): 마지막 변경일이 오늘이면 재변경을 막는다. 이 사용자 한 명의 상태만으로
    // 판단 가능한 생성/수정 시점 불변식이라 서비스 계층이 아닌 엔티티에서 직접 막는다.
    public void changeNickname(String nickname) {
        if (nicknameUpdatedAt != null && nicknameUpdatedAt.toLocalDate().isEqual(LocalDate.now())) {
            throw new UserException(UserErrorCode.NICKNAME_CHANGE_LIMITED);
        }
        this.nickname = nickname;
        this.nicknameUpdatedAt = LocalDateTime.now();
    }

    public void changeBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void withdraw() {
        this.isWithdrawn = true;
        this.withdrawnAt = LocalDateTime.now();
        // unique 제약(nickname) 해제를 위해 탈퇴 시점에 닉네임을 익명화한다.
        this.nickname = "탈퇴한 사용자" + this.id;
    }
}
