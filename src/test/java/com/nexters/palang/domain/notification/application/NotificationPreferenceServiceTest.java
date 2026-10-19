package com.nexters.palang.domain.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.notification.domain.NotificationPreference;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.notification.infrastructure.NotificationPreferenceRepository;
import com.nexters.palang.domain.notification.presentation.dto.NotificationPreferenceResponse;
import com.nexters.palang.domain.notification.presentation.dto.UpdateNotificationPreferenceRequest;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    private NotificationPreferenceService notificationPreferenceService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        notificationPreferenceService = new NotificationPreferenceService(
                notificationPreferenceRepository, userRepository, transactionManager);
    }

    private User user(Long id) {
        User user = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private NotificationPreference preference(
            boolean pushEnabled, boolean activityCommentEnabled, boolean activityLikeEnabled,
            boolean groupInviteJoinEnabled, boolean groupCommentEnabled, boolean groupLikeEnabled,
            boolean groupScheduleEnabled) {
        return NotificationPreference.builder()
                .user(user(1L))
                .pushEnabled(pushEnabled)
                .activityCommentEnabled(activityCommentEnabled)
                .activityLikeEnabled(activityLikeEnabled)
                .groupInviteJoinEnabled(groupInviteJoinEnabled)
                .groupCommentEnabled(groupCommentEnabled)
                .groupLikeEnabled(groupLikeEnabled)
                .groupScheduleEnabled(groupScheduleEnabled)
                .build();
    }

    private UpdateNotificationPreferenceRequest request(
            boolean pushEnabled, boolean activityCommentEnabled, boolean activityLikeEnabled,
            boolean groupInviteJoinEnabled, boolean groupCommentEnabled, boolean groupLikeEnabled,
            boolean groupScheduleEnabled) {
        return new UpdateNotificationPreferenceRequest(
                pushEnabled, activityCommentEnabled, activityLikeEnabled,
                groupInviteJoinEnabled, groupCommentEnabled, groupLikeEnabled, groupScheduleEnabled);
    }

    @Test
    @DisplayName("설정을 변경한 적 없으면 조회 시 7개 전부 true를 반환한다")
    void getSettingsReturnsDefaultWhenNoRow() {
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.empty());

        NotificationPreferenceResponse response = notificationPreferenceService.getSettings(1L);

        assertThat(response.pushEnabled()).isTrue();
        assertThat(response.activityCommentEnabled()).isTrue();
        assertThat(response.activityLikeEnabled()).isTrue();
        assertThat(response.groupInviteJoinEnabled()).isTrue();
        assertThat(response.groupCommentEnabled()).isTrue();
        assertThat(response.groupLikeEnabled()).isTrue();
        assertThat(response.groupScheduleEnabled()).isTrue();
    }

    @Test
    @DisplayName("저장된 설정이 있으면 그 값을 그대로 반환한다")
    void getSettingsReturnsStoredValue() {
        NotificationPreference stored = preference(true, false, true, false, true, false, true);
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.of(stored));

        NotificationPreferenceResponse response = notificationPreferenceService.getSettings(1L);

        assertThat(response.activityCommentEnabled()).isFalse();
        assertThat(response.activityLikeEnabled()).isTrue();
        assertThat(response.groupInviteJoinEnabled()).isFalse();
        assertThat(response.groupCommentEnabled()).isTrue();
        assertThat(response.groupLikeEnabled()).isFalse();
        assertThat(response.groupScheduleEnabled()).isTrue();
    }

    @Test
    @DisplayName("처음 변경하는 설정이면 새 row를 생성한다")
    void updateSettingsCreatesRowWhenNotExists() {
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.empty());
        given(userRepository.getReferenceById(1L)).willReturn(user(1L));

        notificationPreferenceService.updateSettings(1L, request(true, false, true, false, true, false, true));

        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(notificationPreferenceRepository).save(captor.capture());
        assertThat(captor.getValue().isActivityCommentEnabled()).isFalse();
        assertThat(captor.getValue().isGroupInviteJoinEnabled()).isFalse();
        assertThat(captor.getValue().isGroupScheduleEnabled()).isTrue();
    }

    @Test
    @DisplayName("이미 설정이 있으면 저장 대신 기존 row를 갱신한다")
    void updateSettingsUpdatesExistingRow() {
        NotificationPreference existing = preference(true, true, true, true, true, true, true);
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.of(existing));

        notificationPreferenceService.updateSettings(1L, request(false, false, false, false, false, false, false));

        assertThat(existing.isPushEnabled()).isFalse();
        assertThat(existing.isActivityCommentEnabled()).isFalse();
        assertThat(existing.isGroupScheduleEnabled()).isFalse();
        verify(notificationPreferenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("최초 PUT이 동시에 들어와 유니크 제약 위반이 나면, 상대방이 만든 row를 다시 조회해 갱신한다")
    void updateSettingsRetriesWhenConcurrentInsertConflicts() {
        NotificationPreference createdByOtherRequest = preference(true, true, true, true, true, true, true);
        given(notificationPreferenceRepository.findByUserId(1L))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(createdByOtherRequest));
        given(userRepository.getReferenceById(1L)).willReturn(user(1L));
        willThrow(new DataIntegrityViolationException("duplicate key"))
                .given(notificationPreferenceRepository).save(any());

        notificationPreferenceService.updateSettings(1L, request(true, false, true, false, true, false, true));

        assertThat(createdByOtherRequest.isActivityCommentEnabled()).isFalse();
        assertThat(createdByOtherRequest.isGroupInviteJoinEnabled()).isFalse();
    }

    @Test
    @DisplayName("설정이 없는 사용자는 항상 허용으로 취급한다")
    void isEnabledDefaultsToTrueWhenNoRow() {
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThat(notificationPreferenceService.isEnabled(1L, NotificationType.OPINION_LIKED, null)).isTrue();
        assertThat(notificationPreferenceService.isEnabled(1L, NotificationType.GROUP_SCHEDULE_CHANGED, 9L)).isTrue();
    }

    @Test
    @DisplayName("푸시 마스터가 꺼져 있으면 카테고리 설정과 무관하게 차단한다")
    void isEnabledBlocksEverythingWhenPushDisabled() {
        NotificationPreference stored = preference(false, true, true, true, true, true, true);
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.of(stored));

        assertThat(notificationPreferenceService.isEnabled(1L, NotificationType.OPINION_LIKED, null)).isFalse();
        assertThat(notificationPreferenceService.isEnabled(1L, NotificationType.GROUP_JOINED, 9L)).isFalse();
    }

    @Test
    @DisplayName("OPINION_COMMENTED는 groupId 유무로 활동/모임 댓글 설정을 구분한다")
    void isEnabledPicksCommentCategoryByGroupId() {
        NotificationPreference stored = preference(true, false, true, true, true, true, true);
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.of(stored));

        assertThat(notificationPreferenceService.isEnabled(1L, NotificationType.OPINION_COMMENTED, null)).isFalse();
        assertThat(notificationPreferenceService.isEnabled(1L, NotificationType.OPINION_COMMENTED, 9L)).isTrue();
    }

    @Test
    @DisplayName("OPINION_LIKED는 groupId 유무로 활동/모임 좋아요 설정을 구분한다")
    void isEnabledPicksLikeCategoryByGroupId() {
        NotificationPreference stored = preference(true, true, false, true, true, true, true);
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.of(stored));

        assertThat(notificationPreferenceService.isEnabled(1L, NotificationType.OPINION_LIKED, null)).isFalse();
        assertThat(notificationPreferenceService.isEnabled(1L, NotificationType.OPINION_LIKED, 9L)).isTrue();
    }

    @Test
    @DisplayName("GROUP_JOINED/GROUP_MEMBER_JOINED는 모임 초대 및 가입 설정을 따른다")
    void isEnabledUsesGroupInviteJoinCategory() {
        NotificationPreference stored = preference(true, true, true, false, true, true, true);
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.of(stored));

        assertThat(notificationPreferenceService.isEnabled(1L, NotificationType.GROUP_JOINED, 9L)).isFalse();
        assertThat(notificationPreferenceService.isEnabled(1L, NotificationType.GROUP_MEMBER_JOINED, 9L)).isFalse();
    }

    @Test
    @DisplayName("GROUP_SCHEDULE_CHANGED/GROUP_ENDING_SOON은 모임 일정 변경 설정을 따른다")
    void isEnabledUsesGroupScheduleCategory() {
        NotificationPreference stored = preference(true, true, true, true, true, true, false);
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.of(stored));

        assertThat(notificationPreferenceService.isEnabled(1L, NotificationType.GROUP_SCHEDULE_CHANGED, 9L)).isFalse();
        assertThat(notificationPreferenceService.isEnabled(1L, NotificationType.GROUP_ENDING_SOON, 9L)).isFalse();
    }

    @Test
    @DisplayName("BOOK_NEW_OPINIONS는 대응하는 토글이 없어 항상 허용한다")
    void isEnabledAlwaysAllowsBookNewOpinions() {
        NotificationPreference stored = preference(true, false, false, false, false, false, false);
        given(notificationPreferenceRepository.findByUserId(1L)).willReturn(Optional.of(stored));

        assertThat(notificationPreferenceService.isEnabled(1L, NotificationType.BOOK_NEW_OPINIONS, null)).isTrue();
    }
}
