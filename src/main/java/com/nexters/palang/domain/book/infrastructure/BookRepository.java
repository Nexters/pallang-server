package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.domain.Book;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByTitleContainingIgnoreCase(String keyword);
}
