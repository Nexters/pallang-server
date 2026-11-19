package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.group.domain.GroupInvitation;
import com.nexters.palang.domain.group.domain.GroupInvitationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {

    // 초대 미리보기/가입: 응답에 모임 정보를 쓰므로 group과 host까지 join fetch한다(open-in-view: false).
    @Query("select i from GroupInvitation i join fetch i.group g join fetch g.host where i.code = :code")
    Optional<GroupInvitation> findWithGroupByCode(@Param("code") String code);

    // 모임장이 공유할 현재 유효 초대(가장 최근 발급분).
    Optional<GroupInvitation> findFirstByGroupIdAndStatusOrderByIdDesc(Long groupId, GroupInvitationStatus status);

    List<GroupInvitation> findAllByGroupIdAndStatus(Long groupId, GroupInvitationStatus status);

    void deleteAllByGroupIdIn(Collection<Long> groupIds);
}
