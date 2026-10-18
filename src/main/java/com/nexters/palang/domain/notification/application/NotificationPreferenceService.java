package com.nexters.palang.domain.notification.application;

import com.nexters.palang.domain.notification.domain.NotificationPreference;
import com.nexters.palang.domain.notification.infrastructure.NotificationPreferenceRepository;
import com.nexters.palang.domain.notification.presentation.dto.NotificationPreferenceResponse;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserRepository userRepository;

    public NotificationPreferenceResponse getSettings(Long userId) {
        return notificationPreferenceRepository.findByUserId(userId)
                .map(NotificationPreferenceResponse::from)
                .orElseGet(NotificationPreferenceResponse::defaultSettings);
    }

    @Transactional
    public void updateSettings(Long userId, boolean activityEnabled, boolean groupEnabled) {
        notificationPreferenceRepository.findByUserId(userId)
                .ifPresentOrElse(
                        preference -> preference.update(activityEnabled, groupEnabled),
                        () -> {
                            User user = userRepository.getReferenceById(userId);
                            notificationPreferenceRepository.save(NotificationPreference.builder()
                                    .user(user)
                                    .activityEnabled(activityEnabled)
                                    .groupEnabled(groupEnabled)
                                    .build());
                        });
    }

    // groupId가 있으면 모임(GROUP) 알림, 없으면 활동(ACTIVITY) 알림으로 간주한다 — NotificationScope와 동일한 기준.
    public boolean isEnabled(Long userId, Long groupId) {
        return notificationPreferenceRepository.findByUserId(userId)
                .map(preference -> groupId != null ? preference.isGroupEnabled() : preference.isActivityEnabled())
                .orElse(true);
    }
}
