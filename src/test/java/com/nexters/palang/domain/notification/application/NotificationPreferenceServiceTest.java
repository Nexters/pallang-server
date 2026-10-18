package com.nexters.palang.domain.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.notification.domain.NotificationPreference;
import com.nexters.palang.domain.notification.infrastructure.NotificationPreferenceRepository;
import com.nexters.palang.domain.notification.presentation.dto.NotificationPreferenceResponse;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private UserRepository userRepository;

    private NotificationPreferenceService notificationPreferenceService;

    @BeforeEach
    void setUp() {
        notificationPreferenceService = new NotificationPreferenceService(notificationPreferenceRepository, userRepository);
    }

    private User user(Long id) {
        User user = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("설정을 변경한 적 없으면 조회 시 둘 다 true를 반환한다")
    void getSettingsReturnsDefaultWhenNoRow() {
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.empty());

        NotificationPreferenceResponse response = notificationPreferenceService.getSettings(1L);

        assertThat(response.activityEnabled()).isTrue();
        assertThat(response.groupEnabled()).isTrue();
    }

    @Test
    @DisplayName("저장된 설정이 있으면 그 값을 그대로 반환한다")
    void getSettingsReturnsStoredValue() {
        NotificationPreference preference = NotificationPreference.builder()
                .user(user(1L)).activityEnabled(false).groupEnabled(true).build();
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.of(preference));

        NotificationPreferenceResponse response = notificationPreferenceService.getSettings(1L);

        assertThat(response.activityEnabled()).isFalse();
        assertThat(response.groupEnabled()).isTrue();
    }

    @Test
    @DisplayName("처음 변경하는 설정이면 새 row를 생성한다")
    void updateSettingsCreatesRowWhenNotExists() {
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.empty());
        given(userRepository.getReferenceById(1L)).willReturn(user(1L));

        notificationPreferenceService.updateSettings(1L, false, true);

        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(notificationPreferenceRepository).save(captor.capture());
        assertThat(captor.getValue().isActivityEnabled()).isFalse();
        assertThat(captor.getValue().isGroupEnabled()).isTrue();
    }

    @Test
    @DisplayName("이미 설정이 있으면 저장 대신 기존 row를 갱신한다")
    void updateSettingsUpdatesExistingRow() {
        NotificationPreference preference = NotificationPreference.builder()
                .user(user(1L)).activityEnabled(true).groupEnabled(true).build();
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.of(preference));

        notificationPreferenceService.updateSettings(1L, false, false);

        assertThat(preference.isActivityEnabled()).isFalse();
        assertThat(preference.isGroupEnabled()).isFalse();
        verify(notificationPreferenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("설정이 없는 사용자는 groupId 유무와 무관하게 항상 허용으로 취급한다")
    void isEnabledDefaultsToTrueWhenNoRow() {
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThat(notificationPreferenceService.isEnabled(1L, null)).isTrue();
        assertThat(notificationPreferenceService.isEnabled(1L, 9L)).isTrue();
    }

    @Test
    @DisplayName("groupId가 없으면 activityEnabled를, 있으면 groupEnabled를 기준으로 판단한다")
    void isEnabledUsesGroupIdToPickCategory() {
        NotificationPreference preference = NotificationPreference.builder()
                .user(user(1L)).activityEnabled(false).groupEnabled(true).build();
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.of(preference));

        assertThat(notificationPreferenceService.isEnabled(1L, null)).isFalse();
        assertThat(notificationPreferenceService.isEnabled(1L, 9L)).isTrue();
    }
}
