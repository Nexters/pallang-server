package com.nexters.palang.domain.block.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.domain.block.common.BlockException;
import com.nexters.palang.domain.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserBlockTest {

    private User user(Long id) {
        User built = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    @Test
    @DisplayName("서로 다른 사용자를 차단하면 정상적으로 생성된다")
    void createsUserBlockForDifferentUsers() {
        User blocker = user(1L);
        User blocked = user(2L);

        UserBlock userBlock = UserBlock.of(blocker, blocked);

        assertThat(userBlock.getBlocker()).isEqualTo(blocker);
        assertThat(userBlock.getBlocked()).isEqualTo(blocked);
    }

    @Test
    @DisplayName("본인을 차단하려 하면 예외가 발생한다")
    void throwsWhenBlockingSelf() {
        User self = user(1L);

        assertThatThrownBy(() -> UserBlock.of(self, self)).isInstanceOf(BlockException.class);
    }
}
