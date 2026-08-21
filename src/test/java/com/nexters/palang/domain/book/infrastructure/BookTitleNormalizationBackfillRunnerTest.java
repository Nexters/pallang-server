package com.nexters.palang.domain.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

// title_normalized 컬럼이 추가되기 전부터 있던 도서(= @PrePersist를 거치지 않아 이 컬럼이 NULL인
// 도서)를 시뮬레이션하기 위해, 정상적으로 저장한 뒤 벌크 update로 title_normalized만 다시 NULL로
// 되돌린다. Book.builder()로 저장하면 항상 @PrePersist가 채우기 때문에 이렇게 우회해야 한다.
@DataJpaTest
@Import(JpaAuditingConfig.class)
class BookTitleNormalizationBackfillRunnerTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private void clearTitleNormalized(Long id) {
        entityManager.getEntityManager()
                .createQuery("update Book b set b.titleNormalized = null where b.id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

    @Test
    @DisplayName("title_normalized가 NULL인 기존 도서를 앱 시작 시 자동으로 채운다")
    void backfillsBooksWithMissingTitleNormalized() {
        BookTitleNormalizationBackfillRunner runner =
                new BookTitleNormalizationBackfillRunner(bookRepository, transactionManager, 500);

        Book legacyBook = entityManager.persistAndFlush(Book.builder()
                .title("두 번째 산책")
                .author("작가")
                .publisher("출판사")
                .pageCount(300)
                .build());
        clearTitleNormalized(legacyBook.getId());
        entityManager.clear();

        runner.run(null);

        Book reloaded = entityManager.find(Book.class, legacyBook.getId());
        assertThat(reloaded.getTitleNormalized()).isEqualTo("두번째산책");
    }

    @Test
    @DisplayName("이미 title_normalized가 채워진 도서는 건드리지 않는다")
    void doesNotTouchAlreadyNormalizedBooks() {
        BookTitleNormalizationBackfillRunner runner =
                new BookTitleNormalizationBackfillRunner(bookRepository, transactionManager, 500);

        Book book = entityManager.persistAndFlush(Book.builder()
                .title("채식주의자")
                .author("한강")
                .publisher("창비")
                .pageCount(200)
                .build());
        entityManager.clear();

        runner.run(null);

        Book reloaded = entityManager.find(Book.class, book.getId());
        assertThat(reloaded.getTitleNormalized()).isEqualTo("채식주의자");
    }

    @Test
    @DisplayName("배치 크기보다 백필 대상이 많아도 id 기준으로 이어서 조회해 하나도 건너뛰지 않는다")
    void backfillsAllBooksAcrossMultipleBatches() {
        // 배치 크기를 일부러 작게(2) 주입해서, 여러 번 반복 조회해야 하는 상황을 재현한다.
        // offset 페이지네이션이었다면 앞 배치가 채워지며 IS NULL 조건에서 빠져나가 다음 행을
        // 건너뛰었을 텐데, id > lastId 기준 키셋 페이지네이션이라 5권 모두 채워져야 한다.
        BookTitleNormalizationBackfillRunner runner =
                new BookTitleNormalizationBackfillRunner(bookRepository, transactionManager, 2);

        List<Long> legacyBookIds = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> entityManager.persistAndFlush(Book.builder()
                        .title("책" + i)
                        .author("작가")
                        .publisher("출판사")
                        .pageCount(100)
                        .build()))
                .map(Book::getId)
                .toList();
        legacyBookIds.forEach(this::clearTitleNormalized);
        entityManager.clear();

        runner.run(null);

        List<String> normalizedTitles = legacyBookIds.stream()
                .map(id -> entityManager.find(Book.class, id).getTitleNormalized())
                .toList();
        assertThat(normalizedTitles).containsExactly("책1", "책2", "책3", "책4", "책5");
    }
}
