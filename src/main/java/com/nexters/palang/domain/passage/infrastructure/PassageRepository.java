package com.nexters.palang.domain.passage.infrastructure;

import com.nexters.palang.domain.passage.domain.Passage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassageRepository extends JpaRepository<Passage, Long> {

    // 흔적 목록 조회 시 대상 대목의 존재 여부뿐 아니라 group 소속을 확인해야 해서(모임원 검증) 엔티티
    // 자체를 조회한다.
    Optional<Passage> findByIdAndDeletedAtIsNull(Long id);

    // 관리자 유저 삭제(AdminUserService): 이 유저가 작성자인 대목(소속 모임 무관) 전부.
    // 소프트 삭제 여부와 무관하게 물리적으로 존재하는 행을 전부 대상으로 해야 FK 위반 없이 정리할 수 있다.
    List<Passage> findAllByCreatorId(Long userId);

    // 관리자 유저 삭제(AdminUserService): 이 유저가 혼자 호스트인 모임들(soloHostedGroupIds)에 속한 대목 전부.
    List<Passage> findAllByGroupIdIn(List<Long> groupIds);
}
