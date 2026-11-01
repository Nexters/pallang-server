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

// 표지를 카카오 썸네일(120x174) 대신 원본으로 쓰도록 바꾸기 전에, 카카오 검색으로 등록됐거나 알라딘 표지
// 백필로 교체된 도서에는 썸네일 URL(search1.kakaocdn.net/thumb/...)이 저장돼 있다. 이 URL의 fname에서
// 원본 URL을 꺼내 교체한다(KakaoCoverImageUrl). URL 변환만 하고 외부 API는 호출하지 않으므로
// BookTitleNormalizationBackfillRunner처럼 기동 시 동기로 돈다. 교체된 도서는 조건에서 빠지므로 멱등적이다.
//
// 원본을 꺼낼 수 없는 URL은 썸네일 그대로 두고 계속 조건에 걸리므로, offset이 아니라 id 키셋
// 페이지네이션으로 이어서 조회한다.
@Slf4j
@Component
public class KakaoCoverThumbnailBackfillRunner implements ApplicationRunner {

    private static final int DEFAULT_BATCH_SIZE = 500;

    private final BookRepository bookRepository;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;

    @Autowired
    public KakaoCoverThumbnailBackfillRunner(
            BookRepository bookRepository, PlatformTransactionManager transactionManager) {
        this(bookRepository, transactionManager, DEFAULT_BATCH_SIZE);
    }

    KakaoCoverThumbnailBackfillRunner(
            BookRepository bookRepository, PlatformTransactionManager transactionManager, int batchSize) {
        this.bookRepository = bookRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.batchSize = batchSize;
    }

    @Override
    public void run(ApplicationArguments args) {
        Pageable pageable = PageRequest.of(0, batchSize);
        long lastId = 0L;
        int totalReplaced = 0;

        while (true) {
            long afterId = lastId;
            BatchResult result = transactionTemplate.execute(status -> {
                List<Book> batch = bookRepository.findByCoverImageUrlContainingAndIdGreaterThanOrderByIdAsc(
                        KakaoCoverImageUrl.THUMBNAIL_URL_KEYWORD, afterId, pageable);
                int replaced = 0;
                for (Book book : batch) {
                    String original = KakaoCoverImageUrl.toOriginal(book.getCoverImageUrl());
                    if (!original.equals(book.getCoverImageUrl())) {
                        book.replaceCoverImageUrl(original);
                        replaced++;
                    }
                }
                Long batchLastId = batch.isEmpty() ? null : batch.get(batch.size() - 1).getId();
                return new BatchResult(batchLastId, replaced);
            });

            if (result == null || result.lastId() == null) {
                break;
            }
            lastId = result.lastId();
            totalReplaced += result.replaced();
        }

        if (totalReplaced > 0) {
            log.info("카카오 썸네일 표지 원본 교체 완료: {}건", totalReplaced);
        }
    }

    private record BatchResult(Long lastId, int replaced) {
    }
}
