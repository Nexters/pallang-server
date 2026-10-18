package com.nexters.palang.domain.notification.application;

import com.nexters.palang.domain.notification.domain.NotificationPreference;
import com.nexters.palang.domain.notification.infrastructure.NotificationPreferenceRepository;
import com.nexters.palang.domain.notification.presentation.dto.NotificationPreferenceResponse;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserRepository userRepository;
    // 최초 PUT이 동시에 두 번 들어오면 user_id 유니크 제약 위반이 날 수 있다. 이 경우를 별도 트랜잭션으로
    // 재시도하기 위해, self-invocation으로 @Transactional이 무시되는 문제 없이 트랜잭션 경계를 직접 제어한다.
    private final TransactionTemplate newTransactionTemplate;

    public NotificationPreferenceService(
            NotificationPreferenceRepository notificationPreferenceRepository,
            UserRepository userRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.userRepository = userRepository;
        this.newTransactionTemplate = new TransactionTemplate(transactionManager);
        this.newTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getSettings(Long userId) {
        return notificationPreferenceRepository.findByUserId(userId)
                .map(NotificationPreferenceResponse::from)
                .orElseGet(NotificationPreferenceResponse::defaultSettings);
    }

    public void updateSettings(Long userId, boolean activityEnabled, boolean groupEnabled) {
        try {
            newTransactionTemplate.executeWithoutResult(
                    status -> upsert(userId, activityEnabled, groupEnabled));
        } catch (DataIntegrityViolationException e) {
            // 동시에 들어온 최초 PUT끼리 경합해 유니크 제약을 위반한 경우: 상대방이 만든 row를 다시 조회해 갱신한다.
            newTransactionTemplate.executeWithoutResult(
                    status -> notificationPreferenceRepository.findByUserId(userId)
                            .ifPresent(preference -> preference.update(activityEnabled, groupEnabled)));
        }
    }

    private void upsert(Long userId, boolean activityEnabled, boolean groupEnabled) {
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
