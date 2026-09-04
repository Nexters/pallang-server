package com.nexters.palang.domain.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.SampleLibraryBook;
import com.nexters.palang.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class SampleLibraryBookSeederTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository bookRepository;

    @Test
    @DisplayName("샘플 도서가 없는 환경(예: prod)이면 앱 시작 시 ISBN으로 하나 만들어 둔다")
    void createsSampleBookWhenMissing() {
        SampleLibraryBookSeeder seeder = new SampleLibraryBookSeeder(bookRepository);

        seeder.run(null);

        Book created = bookRepository.findByIsbn(SampleLibraryBook.ISBN).orElseThrow();
        assertThat(created.getTitle()).isEqualTo(SampleLibraryBook.TITLE);
    }

    @Test
    @DisplayName("샘플 도서가 이미 있으면(예: dev) 다시 만들지 않는다")
    void doesNotDuplicateWhenAlreadyPresent() {
        Book existing = entityManager.persistAndFlush(Book.builder()
                .title(SampleLibraryBook.TITLE)
                .author(SampleLibraryBook.AUTHOR)
                .publisher(SampleLibraryBook.PUBLISHER)
                .pageCount(SampleLibraryBook.PAGE_COUNT)
                .isbn(SampleLibraryBook.ISBN)
                .build());
        entityManager.clear();
        SampleLibraryBookSeeder seeder = new SampleLibraryBookSeeder(bookRepository);

        seeder.run(null);

        assertThat(bookRepository.count()).isEqualTo(1);
        assertThat(bookRepository.findByIsbn(SampleLibraryBook.ISBN).orElseThrow().getId())
                .isEqualTo(existing.getId());
    }
}
