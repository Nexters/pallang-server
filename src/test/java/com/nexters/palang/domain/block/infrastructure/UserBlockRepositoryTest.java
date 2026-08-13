package com.nexters.palang.domain.block.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.block.domain.UserBlock;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class UserBlockRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserBlockRepository userBlockRepository;

    private User blocker;
    private User blocked;

    @BeforeEach
    void setUp() {
        blocker = user("blocker");
        blocked = user("blocked");
    }

    private User user(String snsId) {
        return entityManager.persistAndFlush(User.builder()
                .nickname("닉네임" + snsId)
                .snsProvider(SnsProvider.KAKAO)
                .snsId(snsId)
                .termsAgreedAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("차단 관계를 저장하면 존재 여부를 조회할 수 있다")
    void existsByBlockerIdAndBlockedIdReturnsTrueAfterSave() {
        entityManager.persistAndFlush(UserBlock.of(blocker, blocked));

        boolean exists = userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("차단 관계가 없으면 존재하지 않는다고 조회된다")
    void existsByBlockerIdAndBlockedIdReturnsFalseWhenNotBlocked() {
        boolean exists = userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("차단을 삭제하면 더 이상 존재하지 않는다")
    void deleteByBlockerIdAndBlockedIdRemovesBlock() {
        entityManager.persistAndFlush(UserBlock.of(blocker, blocked));

        userBlockRepository.deleteByBlockerIdAndBlockedId(blocker.getId(), blocked.getId());
        entityManager.flush();

        assertThat(userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked.getId())).isFalse();
    }
}
