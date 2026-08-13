package com.nexters.palang.domain.block.application;

import com.nexters.palang.domain.block.common.BlockErrorCode;
import com.nexters.palang.domain.block.common.BlockException;
import com.nexters.palang.domain.block.domain.UserBlock;
import com.nexters.palang.domain.block.infrastructure.BlockQueryRepository;
import com.nexters.palang.domain.block.infrastructure.UserBlockRepository;
import com.nexters.palang.domain.user.common.error.UserErrorCode;
import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {

    private final UserBlockRepository userBlockRepository;
    private final BlockQueryRepository blockQueryRepository;
    private final UserRepository userRepository;

    @Transactional
    public void block(Long blockerId, Long blockedUserId) {
        User blocked = userRepository.findById(blockedUserId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedUserId)) {
            throw new BlockException(BlockErrorCode.ALREADY_BLOCKED);
        }
        User blocker = userRepository.getReferenceById(blockerId);
        userBlockRepository.save(UserBlock.of(blocker, blocked));
    }

    @Transactional
    public void unblock(Long blockerId, Long blockedUserId) {
        if (!userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedUserId)) {
            throw new BlockException(BlockErrorCode.BLOCK_NOT_FOUND);
        }
        userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedUserId);
    }

    public Page<UserBlock> getBlockedUsers(Long blockerId, Pageable pageable) {
        return blockQueryRepository.findBlockedUsers(blockerId, pageable);
    }
}
