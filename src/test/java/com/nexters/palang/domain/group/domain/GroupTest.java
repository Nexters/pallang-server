package com.nexters.palang.domain.group.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GroupTest {

    private User user(Long id) {
        User built = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    @Test
    @DisplayName("모임을 만들면 host가 지정된 ACTIVE 모임이 되고, 이름의 앞뒤 공백은 제거된다")
    void createsGroup() {
        Group group = Group.create("  고전 뽀개기 ", user(1L), 4);

        assertThat(group.getHost().getId()).isEqualTo(1L);
        assertThat(group.getName()).isEqualTo("고전 뽀개기");
        assertThat(group.getStatus()).isEqualTo(GroupStatus.ACTIVE);
        assertThat(group.isHost(1L)).isTrue();
        assertThat(group.isHost(2L)).isFalse();
    }

    @Test
    @DisplayName("인원이 2명 미만이면 예외가 발생한다")
    void createFailsWhenCapacityTooSmall() {
        assertThatThrownBy(() -> Group.create("고전 뽀개기", user(1L), 1))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("인원이 10명 초과면 예외가 발생한다")
    void createFailsWhenCapacityTooLarge() {
        assertThatThrownBy(() -> Group.create("고전 뽀개기", user(1L), 11))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("공백만 있는 이름으로는 모임을 만들 수 없다")
    void createFailsWhenNameIsBlank() {
        assertThatThrownBy(() -> Group.create("   ", user(1L), 4))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.INVALID_GROUP_NAME);
    }

    @Test
    @DisplayName("방 설정을 변경하면 이름과 인원이 바뀐다")
    void updatesSettings() {
        Group group = Group.create("고전 뽀개기", user(1L), 4);

        group.updateSettings("주말 독서 모임", 6, 2L);

        assertThat(group.getName()).isEqualTo("주말 독서 모임");
        assertThat(group.getCapacity()).isEqualTo(6);
    }

    @Test
    @DisplayName("현재 참여 인원보다 적은 인원으로 변경하면 예외가 발생한다")
    void updateFailsWhenCapacityBelowMemberCount() {
        Group group = Group.create("고전 뽀개기", user(1L), 4);

        assertThatThrownBy(() -> group.updateSettings("고전 뽀개기", 2, 3L))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.CAPACITY_BELOW_MEMBER_COUNT);
    }

    @Test
    @DisplayName("설정 변경 중 한 필드라도 잘못되면 이름도 바뀌지 않는다")
    void updateChangesNothingWhenAnyFieldIsInvalid() {
        Group group = Group.create("고전 뽀개기", user(1L), 4);

        assertThatThrownBy(() -> group.updateSettings("주말 독서 모임", 2, 3L))
                .isInstanceOf(GroupException.class);

        assertThat(group.getName()).isEqualTo("고전 뽀개기");
        assertThat(group.getCapacity()).isEqualTo(4);
    }
}
