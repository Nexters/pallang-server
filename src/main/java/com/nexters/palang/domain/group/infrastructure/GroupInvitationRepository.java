package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.group.domain.GroupInvitation;
import com.nexters.palang.domain.group.domain.GroupInvitationStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {

    // 초대 미리보기/가입: 응답에 모임 정보를 쓰므로 group과 host까지 join fetch한다(open-in-view: false).
    @Query("select i from GroupInvitation i join fetch i.group g join fetch g.host where i.code = :code")
    Optional<GroupInvitation> findWithGroupByCode(@Param("code") String code);

    // 가입 전용: 초대 코드로 모임 id만 찾는다. 모임 엔티티를 여기서 읽어두면 이후 모임 행 잠금 조회가 영속성 컨텍스트의
    // 옛 상태(정원/상태)를 그대로 돌려주므로, 잠금 전에는 id만 읽는다(GroupService.joinGroup).
    @Query("select i.group.id from GroupInvitation i where i.code = :code")
    Optional<Long> findGroupIdByCode(@Param("code") String code);

    // 가입 전용: 모임 행을 잠근 뒤 초대 상태를 최신 커밋 기준으로 다시 확인한다(그 사이 재발급/종료 반영).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from GroupInvitation i where i.code = :code")
    Optional<GroupInvitation> findByCodeForUpdate(@Param("code") String code);

    // 모임장이 공유할 현재 유효 초대(가장 최근 발급분).
    Optional<GroupInvitation> findFirstByGroupIdAndStatusOrderByIdDesc(Long groupId, GroupInvitationStatus status);

    List<GroupInvitation> findAllByGroupIdAndStatus(Long groupId, GroupInvitationStatus status);

    void deleteAllByGroupIdIn(Collection<Long> groupIds);
}
