package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.domain.Book;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// title_normalized 컬럼은 @PrePersist/@PreUpdate(Book#normalizeTitle)로 채워지는데, 이 컬럼이
// 추가되기 전부터 있던 도서는 title이 바뀔 계기가 없어 영원히 NULL로 남는다. BookQueryRepository의
// 내부 검색이 이 컬럼만 보게 되어 있어서, 백필하지 않으면 기존 도서가 검색 결과에서 사라진다.
// 이 프로젝트에 Flyway 등 마이그레이션 도구가 없어 배포 시 수동 SQL 실행에 기대는 대신, 앱 시작
// 시 한 번 NULL인 도서를 찾아 자동으로 채운다. 채울 대상이 없으면 그냥 아무 일도 하지 않으므로
// 재배포/재기동해도 안전하다(멱등적).
//
// 기존 도서 수가 많을 수 있으므로 전체를 한 번에 메모리에 올리거나 하나의 긴 트랜잭션으로 묶지
// 않고, id 기준 키셋 페이지네이션으로 나눠 배치마다 별도 트랜잭션에서 처리한다.
@Slf4j
@Component
public class BookTitleNormalizationBackfillRunner implements ApplicationRunner {

    private static final int DEFAULT_BATCH_SIZE = 500;

    private final BookRepository bookRepository;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;

    @Autowired
    public BookTitleNormalizationBackfillRunner(
            BookRepository bookRepository, PlatformTransactionManager transactionManager) {
        this(bookRepository, transactionManager, DEFAULT_BATCH_SIZE);
    }

    // 배치 하나로는 끝나지 않는(여러 번 반복되는) 경우까지 검증할 수 있도록 배치 크기를 테스트에서
    // 주입할 수 있게 열어둔 생성자. 프로덕션에서는 위 생성자를 통해 DEFAULT_BATCH_SIZE로 동작한다.
    BookTitleNormalizationBackfillRunner(
            BookRepository bookRepository, PlatformTransactionManager transactionManager, int batchSize) {
        this.bookRepository = bookRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.batchSize = batchSize;
    }

    @Override
    public void run(ApplicationArguments args) {
        Pageable pageable = PageRequest.of(0, batchSize);
        long lastId = 0L;
        int totalBackfilled = 0;

        while (true) {
            long afterId = lastId;
            List<Long> backfilledIds = transactionTemplate.execute(status -> {
                List<Book> batch = bookRepository
                        .findByTitleNormalizedIsNullAndIdGreaterThanOrderByIdAsc(afterId, pageable);
                batch.forEach(Book::backfillTitleNormalizedIfMissing);
                return batch.stream().map(Book::getId).toList();
            });

            if (backfilledIds == null || backfilledIds.isEmpty()) {
                break;
            }
            lastId = backfilledIds.get(backfilledIds.size() - 1);
            totalBackfilled += backfilledIds.size();
        }

        if (totalBackfilled > 0) {
            log.info("title_normalized 백필 완료: {}건", totalBackfilled);
        }
    }
}
