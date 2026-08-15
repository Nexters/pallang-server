package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.domain.Book;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// title_normalized 컬럼은 @PrePersist/@PreUpdate(Book#normalizeTitle)로 채워지는데, 이 컬럼이
// 추가되기 전부터 있던 도서는 title이 바뀔 계기가 없어 영원히 NULL로 남는다. BookQueryRepository의
// 내부 검색이 이 컬럼만 보게 되어 있어서, 백필하지 않으면 기존 도서가 검색 결과에서 사라진다.
// 이 프로젝트에 Flyway 등 마이그레이션 도구가 없어 배포 시 수동 SQL 실행에 기대는 대신, 앱 시작
// 시 한 번 NULL인 도서를 찾아 자동으로 채운다. 채울 대상이 없으면 그냥 아무 일도 하지 않으므로
// 재배포/재기동해도 안전하다(멱등적).
@Slf4j
@Component
@RequiredArgsConstructor
public class BookTitleNormalizationBackfillRunner implements ApplicationRunner {

    private final BookRepository bookRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Book> booksToBackfill = bookRepository.findByTitleNormalizedIsNull();
        if (booksToBackfill.isEmpty()) {
            return;
        }
        booksToBackfill.forEach(Book::backfillTitleNormalizedIfMissing);
        log.info("title_normalized 백필 완료: {}건", booksToBackfill.size());
    }
}
