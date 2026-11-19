package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 멤버십은 탈퇴/내보내기 후에도 행이 남는다(GroupMember.status). 인원 수와 권한 검사는 반드시 ACTIVE만 센다 —
// 아래 *Active* 메서드를 쓰고, 상태 무관 조회는 재가입 처리처럼 이력이 필요한 곳에서만 쓴다.
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    default boolean existsActiveMember(Long groupId, Long userId) {
        return existsByGroupIdAndUserIdAndStatus(groupId, userId, GroupMemberStatus.ACTIVE);
    }

    default long countActiveMembers(Long groupId) {
        return countByGroupIdAndStatus(groupId, GroupMemberStatus.ACTIVE);
    }

    boolean existsByGroupIdAndUserIdAndStatus(Long groupId, Long userId, GroupMemberStatus status);

    long countByGroupIdAndStatus(Long groupId, GroupMemberStatus status);

    // 상태 무관: 재가입 시 기존 행을 되살리는 데 쓴다((group_id, user_id) 유니크).
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    // 모임 알림 팬아웃(일정 변경/종료 임박): 이 모임의 현재 멤버 user id 전체.
    @Query("select m.user.id from GroupMember m where m.group.id = :groupId "
            + "and m.status = com.nexters.palang.domain.group.domain.GroupMemberStatus.ACTIVE")
    List<Long> findActiveUserIdsByGroupId(@Param("groupId") Long groupId);

    void deleteAllByGroupId(Long groupId);

    // 관리자 모임/도서 삭제(AdminGroupService/AdminBookService): 여러 모임의 멤버십을 한 번에 제거한다.
    void deleteAllByGroupIdIn(List<Long> groupIds);

    // 관리자 유저 삭제(AdminUserService): 이 유저의 모든 모임 멤버십(본인이 호스트인 모임 포함)을 제거한다.
    void deleteAllByUserId(Long userId);
}
