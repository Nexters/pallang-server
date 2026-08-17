package com.nexters.palang.domain.opinion.infrastructure;

import com.nexters.palang.domain.opinion.domain.Opinion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpinionRepository extends JpaRepository<Opinion, Long> {

    long countByUserIdAndDeletedAtIsNull(Long userId);

    // 흔적 삭제 시 그 대목에 남은 다른 살아있는 흔적이 있는지 확인하기 위함 (없으면 대목도 함께 삭제).
    boolean existsByPassageIdAndDeletedAtIsNullAndIdNot(Long passageId, Long id);

    // open-in-view: false 환경에서 컨트롤러 단 응답 매핑이 opinion.getUser()/getDecorations()를
    // 참조하므로 트랜잭션 안에서 미리 로딩해야 LazyInitializationException을 피할 수 있다.
    @Query("select distinct o from Opinion o join fetch o.user left join fetch o.decorations where o.id = :id")
    Optional<Opinion> findDetailById(@Param("id") Long id);
}
