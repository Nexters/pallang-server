package com.nexters.palang.domain.block.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.block.domain.UserBlock;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class BlockQueryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    private BlockQueryRepository blockQueryRepository;

    private User blocker;

    @BeforeEach
    void setUp() {
        blockQueryRepository = new BlockQueryRepository(new JPAQueryFactory(entityManager.getEntityManager()));
        blocker = user("blocker");
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
    @DisplayName("차단한 사용자 목록을 최신순으로 조회하고 다른 사람의 차단 목록은 제외한다")
    void findBlockedUsersOrdersByCreatedAtDescendingAndExcludesOtherBlockers() {
        User other = user("other-b");
        User first = user("first-b");
        User second = user("second-b");
        User notMine = user("not-mine");
        entityManager.persistAndFlush(UserBlock.of(blocker, first));
        entityManager.persistAndFlush(UserBlock.of(blocker, second));
        entityManager.persistAndFlush(UserBlock.of(other, notMine));

        Page<UserBlock> result = blockQueryRepository.findBlockedUsers(blocker.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(block -> block.getBlocked().getId())
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    @DisplayName("차단한 사용자가 없으면 빈 목록을 반환한다")
    void findBlockedUsersReturnsEmptyWhenNoBlocks() {
        Page<UserBlock> result = blockQueryRepository.findBlockedUsers(blocker.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
