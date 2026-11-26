package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.group.domain.GroupMemberRemoval;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRemovalRepository extends JpaRepository<GroupMemberRemoval, Long> {

    List<GroupMemberRemoval> findAllByGroupIdAndTargetUserId(Long groupId, Long targetUserId);

    // 관리자 모임 삭제/모임장 모임 삭제: 모임과 함께 감사 이력도 정리한다.
    void deleteAllByGroupIdIn(Collection<Long> groupIds);
}
