package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.application.ExternalBookResult;
import com.nexters.palang.domain.book.domain.Book;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// 알라딘 TTB API 서비스 종료에 대비해, 알라딘 검색으로 등록돼 image.aladin.co.kr 표지를 가진 도서를
// ISBN으로 네이버 책 검색에서 다시 찾아 네이버 표지 URL로 교체한다. BookTitleNormalizationBackfillRunner와
// 같이 마이그레이션 도구 대신 앱 시작 시 자동으로 돌고, 교체된 도서는 조건에서 빠지므로 멱등적이다.
//
// 도서마다 외부 API를 호출해 오래 걸릴 수 있으므로 ApplicationRunner로 기동을 막지 않고, 기동이 끝난 뒤
// 비동기로 실행한다. 네이버에서 못 찾았거나 호출이 실패한 도서는 기존 표지를 그대로 두고 건너뛰며,
// 다음 기동 때 다시 시도한다. 네이버 키가 없는 환경(local, 테스트)에서는 아예 실행하지 않는다.
@Slf4j
@Component
public class BookCoverImageBackfillRunner {

    static final String ALADIN_COVER_HOST = "image.aladin.co.kr";
    private static final int DEFAULT_BATCH_SIZE = 100;

    private final BookRepository bookRepository;
    private final NaverBookApiClient naverBookApiClient;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;

    @Autowired
    public BookCoverImageBackfillRunner(
            BookRepository bookRepository,
            NaverBookApiClient naverBookApiClient,
            PlatformTransactionManager transactionManager) {
        this(bookRepository, naverBookApiClient, transactionManager, DEFAULT_BATCH_SIZE);
    }

    BookCoverImageBackfillRunner(
            BookRepository bookRepository,
            NaverBookApiClient naverBookApiClient,
            PlatformTransactionManager transactionManager,
            int batchSize) {
        this.bookRepository = bookRepository;
        this.naverBookApiClient = naverBookApiClient;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.batchSize = batchSize;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!naverBookApiClient.isConfigured()) {
            return;
        }
        run();
    }

    void run() {
        Pageable pageable = PageRequest.of(0, batchSize);
        long lastId = 0L;
        int replaced = 0;
        int skipped = 0;

        while (true) {
            List<Book> batch = bookRepository
                    .findByCoverImageUrlContainingAndIdGreaterThanOrderByIdAsc(ALADIN_COVER_HOST, lastId, pageable);
            if (batch.isEmpty()) {
                break;
            }

            for (Book book : batch) {
                Optional<String> naverCover = findNaverCover(book);
                if (naverCover.isPresent()) {
                    transactionTemplate.executeWithoutResult(status -> bookRepository.findById(book.getId())
                            .ifPresent(target -> target.replaceCoverImageUrl(naverCover.get())));
                    replaced++;
                } else {
                    skipped++;
                }
            }
            lastId = batch.get(batch.size() - 1).getId();
        }

        if (replaced > 0 || skipped > 0) {
            log.info("알라딘 표지 백필 완료: 교체 {}건, 건너뜀 {}건", replaced, skipped);
        }
    }

    private Optional<String> findNaverCover(Book book) {
        if (book.getIsbn() == null || book.getIsbn().isBlank()) {
            return Optional.empty();
        }
        try {
            return naverBookApiClient.findByIsbn(book.getIsbn().trim())
                    .map(ExternalBookResult::coverImageUrl);
        } catch (RuntimeException e) {
            log.warn("알라딘 표지 백필 중 네이버 조회 실패: bookId={}, isbn={}", book.getId(), book.getIsbn(), e);
            return Optional.empty();
        }
    }
}
