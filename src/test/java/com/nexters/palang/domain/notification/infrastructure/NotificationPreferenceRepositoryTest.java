package com.nexters.palang.domain.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.notification.domain.NotificationPreference;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class NotificationPreferenceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    private User user(String snsId) {
        return entityManager.persistAndFlush(User.builder()
                .nickname("닉네임" + snsId)
                .snsProvider(SnsProvider.KAKAO)
                .snsId(snsId)
                .build());
    }

    @Test
    @DisplayName("userId로 저장된 알림 설정을 조회한다")
    void findByUserId() {
        User user = user("user1");
        NotificationPreference preference = entityManager.persistAndFlush(NotificationPreference.builder()
                .user(user)
                .pushEnabled(true)
                .activityCommentEnabled(false)
                .activityLikeEnabled(true)
                .groupInviteJoinEnabled(false)
                .groupCommentEnabled(true)
                .groupLikeEnabled(false)
                .groupScheduleEnabled(true)
                .build());

        Optional<NotificationPreference> found = notificationPreferenceRepository.findByUserId(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(preference.getId());
        assertThat(found.get().isPushEnabled()).isTrue();
        assertThat(found.get().isActivityCommentEnabled()).isFalse();
        assertThat(found.get().isActivityLikeEnabled()).isTrue();
        assertThat(found.get().isGroupInviteJoinEnabled()).isFalse();
        assertThat(found.get().isGroupCommentEnabled()).isTrue();
        assertThat(found.get().isGroupLikeEnabled()).isFalse();
        assertThat(found.get().isGroupScheduleEnabled()).isTrue();
    }

    @Test
    @DisplayName("설정을 저장한 적 없는 사용자는 조회 결과가 비어있다")
    void findByUserIdReturnsEmptyWhenNotSaved() {
        User user = user("user2");

        Optional<NotificationPreference> found = notificationPreferenceRepository.findByUserId(user.getId());

        assertThat(found).isEmpty();
    }
}
