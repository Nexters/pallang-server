package com.nexters.palang.domain.notification.application;

import com.nexters.palang.domain.notification.domain.NotificationPreference;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.notification.infrastructure.NotificationPreferenceRepository;
import com.nexters.palang.domain.notification.presentation.dto.NotificationPreferenceResponse;
import com.nexters.palang.domain.notification.presentation.dto.UpdateNotificationPreferenceRequest;
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

    public void updateSettings(Long userId, UpdateNotificationPreferenceRequest request) {
        try {
            newTransactionTemplate.executeWithoutResult(status -> upsert(userId, request));
        } catch (DataIntegrityViolationException e) {
            // 동시에 들어온 최초 PUT끼리 경합해 유니크 제약을 위반한 경우: 상대방이 만든 row를 다시 조회해 갱신한다.
            newTransactionTemplate.executeWithoutResult(
                    status -> notificationPreferenceRepository.findByUserId(userId)
                            .ifPresent(preference -> applyUpdate(preference, request)));
        }
    }

    private void upsert(Long userId, UpdateNotificationPreferenceRequest request) {
        notificationPreferenceRepository.findByUserId(userId)
                .ifPresentOrElse(
                        preference -> applyUpdate(preference, request),
                        () -> {
                            User user = userRepository.getReferenceById(userId);
                            notificationPreferenceRepository.save(NotificationPreference.builder()
                                    .user(user)
                                    .pushEnabled(request.pushEnabled())
                                    .activityCommentEnabled(request.activityCommentEnabled())
                                    .activityLikeEnabled(request.activityLikeEnabled())
                                    .groupInviteJoinEnabled(request.groupInviteJoinEnabled())
                                    .groupCommentEnabled(request.groupCommentEnabled())
                                    .groupLikeEnabled(request.groupLikeEnabled())
                                    .groupScheduleEnabled(request.groupScheduleEnabled())
                                    .build());
                        });
    }

    private void applyUpdate(NotificationPreference preference, UpdateNotificationPreferenceRequest request) {
        preference.update(
                request.pushEnabled(),
                request.activityCommentEnabled(),
                request.activityLikeEnabled(),
                request.groupInviteJoinEnabled(),
                request.groupCommentEnabled(),
                request.groupLikeEnabled(),
                request.groupScheduleEnabled());
    }

    // 푸시 마스터가 꺼져 있으면 무조건 차단한다. 켜져 있으면 (type, groupId) 조합으로 세부 카테고리를 판단한다
    // — groupId 유무는 NotificationScope와 동일한 기준(활동=전역, 모임=groupId 있음).
    public boolean isEnabled(Long userId, NotificationType type, Long groupId) {
        return notificationPreferenceRepository.findByUserId(userId)
                .map(preference -> preference.isPushEnabled() && isCategoryEnabled(preference, type, groupId))
                .orElse(true);
    }

    private boolean isCategoryEnabled(NotificationPreference preference, NotificationType type, Long groupId) {
        return switch (type) {
            case OPINION_COMMENTED -> groupId != null
                    ? preference.isGroupCommentEnabled() : preference.isActivityCommentEnabled();
            case OPINION_LIKED -> groupId != null
                    ? preference.isGroupLikeEnabled() : preference.isActivityLikeEnabled();
            case GROUP_JOINED, GROUP_MEMBER_JOINED -> preference.isGroupInviteJoinEnabled();
            case GROUP_SCHEDULE_CHANGED, GROUP_ENDING_SOON -> preference.isGroupScheduleEnabled();
            // 더 이상 새로 발송하지 않는다(이슈 #193) — 대응하는 토글이 없으므로 차단하지 않는다.
            case BOOK_NEW_OPINIONS -> true;
            // 운영 안내는 설정으로 끌 수 없다(인앱은 항상 저장). 이 경로를 타지 않지만 switch 완결성을 위해 둔다.
            case CONTENT_REMOVED -> true;
        };
    }

    // 운영 안내처럼 인앱 저장은 항상 하되 푸시만 사용자의 푸시 마스터 설정을 따르는 경우에 쓴다.
    public boolean isPushEnabled(Long userId) {
        return notificationPreferenceRepository.findByUserId(userId)
                .map(NotificationPreference::isPushEnabled)
                .orElse(true);
    }
}
