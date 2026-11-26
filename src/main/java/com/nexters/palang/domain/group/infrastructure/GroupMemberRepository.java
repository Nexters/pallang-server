package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // ===== 모임 행 잠금(GroupRepository.findByIdForUpdate) 뒤에 쓰는 잠금 읽기 =====
    // MySQL 기본 격리 수준(REPEATABLE READ)에서 일반 조회는 트랜잭션의 첫 일반 조회 시점 스냅샷을 읽는다. 모임 행 잠금을
    // 기다리는 사이 다른 트랜잭션이 커밋한 가입/내보내기를 보려면, 판정에 쓰는 멤버십은 잠금 읽기(FOR UPDATE)로 최신 커밋
    // 상태를 읽어야 한다(정원 초과, 중복 내보내기 방지).

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from GroupMember m where m.group.id = :groupId and m.user.id = :userId")
    Optional<GroupMember> findByGroupIdAndUserIdForUpdate(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from GroupMember m where m.group.id = :groupId "
            + "and m.status = com.nexters.palang.domain.group.domain.GroupMemberStatus.ACTIVE")
    List<GroupMember> findActiveMembersForUpdate(@Param("groupId") Long groupId);

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
