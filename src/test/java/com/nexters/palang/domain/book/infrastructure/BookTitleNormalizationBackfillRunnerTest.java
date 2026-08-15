package com.nexters.palang.domain.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

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

    private BookTitleNormalizationBackfillRunner runner;

    @Test
    @DisplayName("title_normalized가 NULL인 기존 도서를 앱 시작 시 자동으로 채운다")
    void backfillsBooksWithMissingTitleNormalized() {
        runner = new BookTitleNormalizationBackfillRunner(bookRepository);

        Book legacyBook = entityManager.persistAndFlush(Book.builder()
                .title("두 번째 산책")
                .author("작가")
                .publisher("출판사")
                .pageCount(300)
                .build());
        entityManager.getEntityManager()
                .createQuery("update Book b set b.titleNormalized = null where b.id = :id")
                .setParameter("id", legacyBook.getId())
                .executeUpdate();
        entityManager.clear();

        runner.run(null);

        Book reloaded = entityManager.find(Book.class, legacyBook.getId());
        assertThat(reloaded.getTitleNormalized()).isEqualTo("두번째산책");
    }

    @Test
    @DisplayName("이미 title_normalized가 채워진 도서는 건드리지 않는다")
    void doesNotTouchAlreadyNormalizedBooks() {
        runner = new BookTitleNormalizationBackfillRunner(bookRepository);

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
}
