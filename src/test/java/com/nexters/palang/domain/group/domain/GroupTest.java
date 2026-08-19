package com.nexters.palang.domain.group.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.user.domain.User;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GroupTest {

    private Book book() {
        Book built = Book.builder()
                .title("채식주의자").author("한강").publisher("창비").pageCount(268).source(BookSource.API).build();
        ReflectionTestUtils.setField(built, "id", 1L);
        return built;
    }

    private User user(Long id) {
        User built = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    @Test
    @DisplayName("모임을 만들면 host가 지정된 채로 생성되고 초대 코드가 발급된다")
    void createsGroup() {
        Group group = Group.create(
                "고전 뽀개기", book(), user(1L), 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));

        assertThat(group.getHost().getId()).isEqualTo(1L);
        assertThat(group.getInviteCode()).isNotBlank();
        assertThat(group.isEnded()).isFalse();
    }

    @Test
    @DisplayName("인원이 2명 미만이면 예외가 발생한다")
    void createFailsWhenCapacityTooSmall() {
        assertThatThrownBy(() -> Group.create(
                "고전 뽀개기", book(), user(1L), 1, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20)))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("인원이 10명 초과면 예외가 발생한다")
    void createFailsWhenCapacityTooLarge() {
        assertThatThrownBy(() -> Group.create(
                "고전 뽀개기", book(), user(1L), 11, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20)))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 예외가 발생한다")
    void createFailsWhenPeriodInvalid() {
        assertThatThrownBy(() -> Group.create(
                "고전 뽀개기", book(), user(1L), 4, LocalDate.of(2026, 9, 20), LocalDate.of(2026, 8, 20)))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("방 설정을 변경하면 이름/인원/기간이 바뀐다")
    void updatesSettings() {
        Group group = Group.create(
                "고전 뽀개기", book(), user(1L), 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));

        group.updateSettings("주말 독서 모임", 6, LocalDate.of(2026, 8, 21), LocalDate.of(2026, 9, 27), 2L);

        assertThat(group.getName()).isEqualTo("주말 독서 모임");
        assertThat(group.getCapacity()).isEqualTo(6);
        assertThat(group.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 21));
        assertThat(group.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 27));
    }

    @Test
    @DisplayName("현재 참여 인원보다 적은 인원으로 변경하면 예외가 발생한다")
    void updateFailsWhenCapacityBelowMemberCount() {
        Group group = Group.create(
                "고전 뽀개기", book(), user(1L), 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));

        assertThatThrownBy(() -> group.updateSettings(
                "고전 뽀개기", 2, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20), 3L))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("종료일이 지나면 종료된 모임으로 판단한다")
    void isEndedReturnsTrueAfterEndDate() {
        Group group = Group.create(
                "고전 뽀개기", book(), user(1L), 4, LocalDate.now().minusDays(10), LocalDate.now().minusDays(1));

        assertThat(group.isEnded()).isTrue();
    }

    @Test
    @DisplayName("초대 코드를 재발급하면 기존 코드와 달라진다")
    void regeneratesInviteCode() {
        Group group = Group.create(
                "고전 뽀개기", book(), user(1L), 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));
        String originalInviteCode = group.getInviteCode();

        group.regenerateInviteCode();

        assertThat(group.getInviteCode()).isNotBlank().isNotEqualTo(originalInviteCode);
    }
}
