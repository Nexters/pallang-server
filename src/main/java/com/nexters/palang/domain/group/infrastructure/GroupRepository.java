package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.group.domain.Group;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface GroupRepository extends JpaRepository<Group, Long> {

    // 관리자 유저 삭제(AdminUserService): 이 유저가 호스트인 모임을 전부 조회해, 다른 멤버가 있는
    // 모임인지(삭제 차단) / 혼자뿐인 모임인지(함께 삭제 가능)를 판별하는 데 쓴다.
    List<Group> findAllByHostId(Long hostId);

    // 관리자 페이지 유저 목록에서 "호스트인 모임 수"를 보여주기 위한 참고용 카운트.
    long countByHostId(Long hostId);

    // 관리자 모임 검색(AdminGroupService): 모임명에 키워드가 포함된 모임 전부.
    // open-in-view: false 환경에서 AdminGroupMapper가 컨트롤러 단(트랜잭션 밖)에서 host를
    // 참조하므로, EntityGraph로 미리 로딩해두지 않으면 LazyInitializationException이 난다.
    @EntityGraph(attributePaths = {"host"})
    Page<Group> findByNameContaining(String keyword, Pageable pageable);

    // 관리자 모임 수정/삭제(AdminGroupService): 위와 같은 이유로, 단건 조회에도 host를 미리
    // 로딩해둔다(수정 응답도 컨트롤러 단에서 같은 필드를 참조한다).
    @EntityGraph(attributePaths = {"host"})
    Optional<Group> findWithAssociationsById(Long id);

    // 모임 상세 조회 전용. GroupMapper.toDetailResponse가 host 필드를 컨트롤러 단에서 읽으므로
    // (open-in-view: false) join fetch로 함께 로딩한다.
    @Query("select g from Group g join fetch g.host where g.id = :groupId")
    Optional<Group> findByIdWithHost(Long groupId);

    // 가입/모임 설정 변경 전용 비관적 락. 정원 확인과 GroupMember 추가(가입), 현재 인원 확인과 정원 변경(설정)
    // 사이에 다른 트랜잭션이 끼어들어 "참여 인원 > 정원"이 되는 것(TOCTOU)을 막기 위해 두 경로가 같은 Group
    // row 잠금을 공유한다. 응답에 필요한 host도 함께 join fetch한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Group g join fetch g.host where g.id = :groupId")
    Optional<Group> findByIdForUpdate(Long groupId);
}
