package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.domain.Book;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {

    // title_normalized 백필 대상(BookTitleNormalizationBackfillRunner) 조회용. 기존 도서가 많을 때
    // 한 번에 전부 불러오지 않도록, id 기준 키셋 페이지네이션으로 한 배치씩만 조회한다. offset
    // 페이지네이션을 쓰면 이전 배치가 채워지며 IS NULL 조건에서 빠져나가 다음 행을 건너뛸 수 있어
    // id > :id 조건으로 이어서 조회해야 한다(pageable은 정렬/LIMIT 용도로만 사용, offset은 항상 0).
    List<Book> findByTitleNormalizedIsNullAndIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);
}
