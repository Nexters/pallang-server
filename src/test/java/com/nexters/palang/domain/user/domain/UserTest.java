package com.nexters.palang.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.domain.user.common.error.UserException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserTest {

    private User user() {
        return User.builder().nickname("기존닉네임").build();
    }

    @Test
    @DisplayName("닉네임을 변경한 적이 없으면 바로 변경할 수 있다")
    void changeNicknameSucceedsWhenNeverChangedBefore() {
        User user = user();

        user.changeNickname("새닉네임");

        assertThat(user.getNickname()).isEqualTo("새닉네임");
        assertThat(user.getNicknameUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("같은 날짜에 닉네임을 다시 변경하려 하면 예외가 발생한다")
    void changeNicknameFailsWhenChangedAgainOnSameDay() {
        User user = user();
        user.changeNickname("새닉네임");

        assertThatThrownBy(() -> user.changeNickname("또다른닉네임")).isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("전날 닉네임을 변경했다면 오늘 다시 변경할 수 있다")
    void changeNicknameSucceedsWhenLastChangedOnPreviousDay() {
        User user = user();
        ReflectionTestUtils.setField(user, "nicknameUpdatedAt", LocalDateTime.now().minusDays(1));

        user.changeNickname("새닉네임");

        assertThat(user.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("배경색을 변경하면 필드가 갱신된다")
    void changeBackgroundColorUpdatesField() {
        User user = user();

        user.changeBackgroundColor("#FFFFFF");

        assertThat(user.getBackgroundColor()).isEqualTo("#FFFFFF");
    }

    @Test
    @DisplayName("가입 직후에는 약관에 동의하지 않은 상태다")
    void termsAgreedAtIsNullRightAfterSignUp() {
        User user = user();

        assertThat(user.getTermsAgreedAt()).isNull();
    }

    @Test
    @DisplayName("약관에 동의하면 동의 시각이 기록된다")
    void agreeToTermsRecordsTimestamp() {
        User user = user();

        user.agreeToTerms();

        assertThat(user.getTermsAgreedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 약관에 동의한 사용자가 다시 동의해도 에러 없이 시각만 갱신된다")
    void agreeToTermsIsIdempotent() {
        User user = user();
        user.agreeToTerms();
        LocalDateTime firstAgreedAt = user.getTermsAgreedAt();

        user.agreeToTerms();

        assertThat(user.getTermsAgreedAt()).isNotNull().isAfterOrEqualTo(firstAgreedAt);
    }

    @Test
    @DisplayName("온보딩을 완료하면 완료 상태가 된다")
    void completeOnboardingMarksCompleted() {
        User user = user();

        user.completeOnboarding();

        assertThat(user.isHasCompletedOnboarding()).isTrue();
    }

    @Test
    @DisplayName("탈퇴하면 닉네임과 snsId가 익명화되어 같은 SNS 계정으로 재가입할 수 있게 된다")
    void withdrawAnonymizesNicknameAndSnsId() {
        User user = User.builder().nickname("기존닉네임").snsProvider(SnsProvider.KAKAO).snsId("kakao-123").build();
        ReflectionTestUtils.setField(user, "id", 42L);

        user.withdraw();

        assertThat(user.isWithdrawn()).isTrue();
        assertThat(user.getWithdrawnAt()).isNotNull();
        assertThat(user.getNickname()).isEqualTo("탈퇴한 사용자42");
        assertThat(user.getSnsId()).isEqualTo("withdrawn:42:kakao-123");
    }
}
