package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.group.domain.Group;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface GroupRepository extends JpaRepository<Group, Long> {

    // 초대 링크 미리보기 등 단순 조회용. 잠금이 필요 없는 경로에서 사용한다.
    Optional<Group> findByInviteCode(String inviteCode);

    // 가입(join) 전용. 정원 초과 여부를 확인하고 GroupMember를 추가하는 사이 다른 트랜잭션이 끼어들어
    // 정원을 넘기는 것(TOCTOU)을 막기 위해 Group row에 비관적 락을 건다. 같은 모임에 대한 동시 가입
    // 요청은 이 락으로 직렬화되고, 락을 먼저 잡은 트랜잭션이 커밋(또는 롤백)해야 다음 요청이 최신
    // 참여 인원 수를 보고 진행한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Group g where g.inviteCode = :inviteCode")
    Optional<Group> findByInviteCodeForUpdate(String inviteCode);

    // 방 설정 변경(정원 변경) 전용. findByInviteCodeForUpdate와 같은 Group row 잠금을 공유해야
    // 두 경로가 서로를 배제한다 — 그렇지 않으면 host가 정원을 줄이는 사이 다른 사용자가 가입해
    // "참여 인원 > 정원"인 모임이 만들어질 수 있다(TOCTOU). 다른 PK로 조회해도 잠그는 row가 같으면
    // DB가 알아서 직렬화하므로, findByInviteCodeForUpdate와 조회 키가 달라도 안전하다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Group g where g.id = :groupId")
    Optional<Group> findByIdForUpdate(Long groupId);
}
