package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.group.domain.GroupMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    long countByGroupId(Long groupId);

    // 모임 알림 팬아웃(일정 변경/종료 임박): 이 모임의 멤버 user id 전체.
    @Query("select m.user.id from GroupMember m where m.group.id = :groupId")
    List<Long> findUserIdsByGroupId(@Param("groupId") Long groupId);

    void deleteAllByGroupId(Long groupId);

    // 관리자 모임/도서 삭제(AdminGroupService/AdminBookService): 여러 모임의 멤버십을 한 번에 제거한다.
    void deleteAllByGroupIdIn(List<Long> groupIds);

    // 관리자 유저 삭제(AdminUserService): 이 유저의 모든 모임 멤버십(본인이 호스트인 모임 포함)을 제거한다.
    void deleteAllByUserId(Long userId);
}
