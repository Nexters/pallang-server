package com.nexters.palang.domain.passage.infrastructure;

import com.nexters.palang.domain.passage.domain.Passage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassageRepository extends JpaRepository<Passage, Long> {

    // 흔적 목록 조회 시 대상 대목의 존재 여부뿐 아니라 group 소속을 확인해야 해서(모임원 검증) 엔티티
    // 자체를 조회한다.
    Optional<Passage> findByIdAndDeletedAtIsNull(Long id);
}
