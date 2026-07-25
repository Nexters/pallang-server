package com.nexters.palang.domain.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    @DisplayName("제목에 포함된 단어로 검색하면 대소문자 구분 없이 도서를 찾는다")
    void findByTitleContainingIgnoreCase() {
        bookRepository.save(Book.builder()
                .title("프랑켄슈타인")
                .author("메리 셸리")
                .publisher("문학동네")
                .pageCount(550)
                .build());
        bookRepository.save(Book.builder()
                .title("드라큘라")
                .author("브램 스토커")
                .publisher("황금가지")
                .pageCount(480)
                .build());

        Page<Book> results = bookRepository.findByTitleContainingIgnoreCase("프랑켄", PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(Book::getTitle).containsExactly("프랑켄슈타인");
        assertThat(results.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("검색어를 포함하는 도서가 없으면 빈 목록을 반환한다")
    void findByTitleContainingIgnoreCaseReturnsEmptyWhenNoMatch() {
        Page<Book> results = bookRepository.findByTitleContainingIgnoreCase("없는책", PageRequest.of(0, 20));

        assertThat(results.getContent()).isEmpty();
    }
}
