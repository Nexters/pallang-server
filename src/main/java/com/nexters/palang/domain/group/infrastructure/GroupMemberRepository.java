package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.group.domain.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    long countByGroupId(Long groupId);

    void deleteAllByGroupId(Long groupId);

    // 관리자 유저 삭제(AdminUserService): 이 유저의 모든 모임 멤버십(본인이 호스트인 모임 포함)을 제거한다.
    void deleteAllByUserId(Long userId);
}
