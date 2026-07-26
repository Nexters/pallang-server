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
}
