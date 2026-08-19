package com.nexters.palang.domain.opinion.infrastructure;

import com.nexters.palang.domain.opinion.domain.Opinion;
import java.util.List;
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

    // "책에 새 의견 N개" 알림(BookNewOpinionsNotifier)의 현재 카운트 기준값.
    long countByPassage_Book_IdAndDeletedAtIsNull(Long bookId);

    // 위 알림의 수신 대상 후보: 이 책에 살아있는 의견을 남긴 적 있는 사용자 목록(작성자 본인 제외는 호출부 책임).
    @Query("select distinct o.user.id from Opinion o where o.passage.book.id = :bookId and o.deletedAt is null")
    List<Long> findDistinctUserIdsByBookId(@Param("bookId") Long bookId);
}
