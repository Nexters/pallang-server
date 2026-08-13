package com.nexters.palang.domain.opinion.infrastructure;

import com.nexters.palang.domain.opinion.domain.Opinion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpinionRepository extends JpaRepository<Opinion, Long> {

    long countByUserIdAndDeletedAtIsNull(Long userId);

    // open-in-view: false 환경에서 컨트롤러 단 응답 매핑이 opinion.getUser()/getDecorations()를
    // 참조하므로 트랜잭션 안에서 미리 로딩해야 LazyInitializationException을 피할 수 있다.
    @Query("select distinct o from Opinion o join fetch o.user left join fetch o.decorations where o.id = :id")
    Optional<Opinion> findDetailById(@Param("id") Long id);
}
