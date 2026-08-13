package com.nexters.palang.domain.block.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.block.common.BlockException;
import com.nexters.palang.domain.block.infrastructure.BlockQueryRepository;
import com.nexters.palang.domain.block.infrastructure.UserBlockRepository;
import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private BlockQueryRepository blockQueryRepository;

    @Mock
    private UserRepository userRepository;

    private BlockService blockService;

    private User blocker;
    private User target;

    @BeforeEach
    void setUp() {
        blockService = new BlockService(userBlockRepository, blockQueryRepository, userRepository);
        blocker = user(1L);
        target = user(2L);
    }

    private User user(Long id) {
        User built = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 차단하면 예외가 발생한다")
    void blockFailsWhenTargetNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> blockService.block(blocker.getId(), 999L)).isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("이미 차단한 사용자를 다시 차단하면 예외가 발생한다")
    void blockFailsWhenAlreadyBlocked() {
        given(userRepository.findById(target.getId())).willReturn(Optional.of(target));
        given(userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), target.getId())).willReturn(true);

        assertThatThrownBy(() -> blockService.block(blocker.getId(), target.getId())).isInstanceOf(BlockException.class);
    }

    @Test
    @DisplayName("본인을 차단하려 하면 예외가 발생한다")
    void blockFailsWhenSelfBlock() {
        given(userRepository.findById(blocker.getId())).willReturn(Optional.of(blocker));
        given(userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocker.getId())).willReturn(false);
        given(userRepository.getReferenceById(blocker.getId())).willReturn(blocker);

        assertThatThrownBy(() -> blockService.block(blocker.getId(), blocker.getId())).isInstanceOf(BlockException.class);
    }

    @Test
    @DisplayName("차단 해제할 내역이 없으면 예외가 발생한다")
    void unblockFailsWhenNotFound() {
        given(userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), target.getId())).willReturn(false);

        assertThatThrownBy(() -> blockService.unblock(blocker.getId(), target.getId())).isInstanceOf(BlockException.class);
        verify(userBlockRepository, never()).deleteByBlockerIdAndBlockedId(blocker.getId(), target.getId());
    }

    @Test
    @DisplayName("차단 내역이 있으면 차단을 해제한다")
    void unblockDeletesExistingBlock() {
        given(userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), target.getId())).willReturn(true);

        blockService.unblock(blocker.getId(), target.getId());

        verify(userBlockRepository).deleteByBlockerIdAndBlockedId(blocker.getId(), target.getId());
    }
}
