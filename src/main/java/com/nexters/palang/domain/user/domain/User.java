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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
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

    // 실명. 카카오는 제공하지 않고, 애플은 최초 로그인 시에만 내려줘서 null일 수 있다. 익명 서비스 특성상
    // 어디에도 노출하지 않고 보관만 한다(FR-AUTH-04 랜덤 닉네임과는 별개).
    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "nickname_updated_at")
    private LocalDateTime nicknameUpdatedAt;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    // 카카오계정(이메일) 동의항목을 거부했거나 미인증 상태면 null일 수 있어 nullable이다.
    @Column(name = "email")
    private String email;

    @Column(name = "background_color", length = BACKGROUND_COLOR_MAX_LENGTH)
    private String backgroundColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "sns_provider", nullable = false)
    private SnsProvider snsProvider;

    // Apple sub는 최대 255자까지 가능해 컬럼 길이를 명시적으로 맞춘다. 탈퇴 시 anonymizeSnsId()가
    // "withdrawn:" + SHA-256(64자) 형태의 고정 길이 값으로 치환하므로 원본 길이와 무관하게 안전하다.
    @Column(name = "sns_id", nullable = false, length = 255)
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

    // 회원탈퇴 시 애플 연동 해제(revoke)에 쓴다. authorizationCode는 발급 후 수 분 내에만 교환 가능해
    // 탈퇴 시점이 아니라 로그인 시점에 미리 교환해 저장해둔다.
    @Column(name = "apple_refresh_token", columnDefinition = "TEXT")
    private String appleRefreshToken;

    @Builder
    private User(
            String nickname,
            String name,
            String profileImageUrl,
            String backgroundColor,
            SnsProvider snsProvider,
            String snsId,
            String email,
            LocalDateTime termsAgreedAt
    ) {
        this.nickname = nickname;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.backgroundColor = backgroundColor;
        this.snsProvider = snsProvider;
        this.snsId = snsId;
        this.email = email;
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

    public void changeProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    // 이메일 동의를 나중에 하는 경우가 있어, 로그인 시점마다 카카오가 내려준 값으로 갱신한다(멱등).
    public void changeEmail(String email) {
        this.email = email;
    }

    public void updateAppleRefreshToken(String appleRefreshToken) {
        this.appleRefreshToken = appleRefreshToken;
    }

    public void withdraw() {
        this.isWithdrawn = true;
        this.withdrawnAt = LocalDateTime.now();
        // unique 제약(nickname) 해제를 위해 탈퇴 시점에 닉네임을 익명화한다.
        this.nickname = "탈퇴한 사용자" + this.id;
        // unique 제약(sns_provider+sns_id) 해제를 위해 이 row(탈퇴한 계정)만의 snsId를 고정 길이 해시로
        // 되돌릴 수 없게 바꾼다. registerViaSns로 만들어지는 새 계정의 snsId에는 관여하지 않는다.
        // 같은 SNS 계정으로 재로그인하면 findBySnsProviderAndSnsId가 원본 snsId로는 이 row를 더 이상
        // 찾지 못해 신규 가입 경로를 타므로, 탈퇴는 되돌릴 수 없는 익명화로 유지되고 재가입은 새 계정으로 시작한다.
        this.snsId = anonymizeSnsId(this.id, this.snsId);
    }

    // Apple sub(최대 255자)까지 고려하면 "withdrawn:" + 원본을 그대로 이어붙일 경우 sns_id 컬럼(varchar(255))을
    // 넘길 수 있어, SHA-256 해시(64자 고정)로 치환해 원본 길이와 무관하게 컬럼 길이 안에 들어오도록 한다.
    private static String anonymizeSnsId(Long id, String originalSnsId) {
        return "withdrawn:" + sha256(id + ":" + originalSnsId);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
