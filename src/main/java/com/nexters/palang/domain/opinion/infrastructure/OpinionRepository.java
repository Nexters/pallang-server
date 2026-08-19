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

    // 대목 소유자 판정(findMyPassages와 동일 기준): 이 대목에 흔적을 남긴 사용자인지 확인.
    boolean existsByPassageIdAndUserIdAndDeletedAtIsNull(Long passageId, Long userId);

    // open-in-view: false 환경에서 컨트롤러 단 응답 매핑이 opinion.getUser()/getDecorations()를
    // 참조하므로 트랜잭션 안에서 미리 로딩해야 LazyInitializationException을 피할 수 있다.
    // passage/passage.group도 함께 fetch join한다 — OpinionService.getOpinion()이 모임 스코프 흔적인지
    // 판단하려면 group을 초기화해야 하는데, 서비스 메서드 트랜잭션이 끝난 뒤(컨트롤러 매핑 시점)에는
    // 지연 로딩이 막혀 있어 이 시점에 함께 가져와야 한다.
    @Query("select distinct o from Opinion o join fetch o.user left join fetch o.decorations "
            + "left join fetch o.passage p left join fetch p.group where o.id = :id")
    Optional<Opinion> findDetailById(@Param("id") Long id);
}
