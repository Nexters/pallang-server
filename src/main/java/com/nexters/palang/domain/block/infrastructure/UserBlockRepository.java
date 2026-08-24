package com.nexters.palang.domain.block.infrastructure;

import com.nexters.palang.domain.block.domain.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    // 관리자 유저 삭제(AdminUserService): 이 유저가 관련된 차단 내역 전부(차단한 쪽/차단당한 쪽 모두).
    void deleteAllByBlockerIdOrBlockedId(Long blockerId, Long blockedId);
}
