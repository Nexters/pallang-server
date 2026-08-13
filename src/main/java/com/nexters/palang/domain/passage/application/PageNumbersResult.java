package com.nexters.palang.domain.passage.application;

import com.nexters.palang.domain.book.domain.Book;
import org.springframework.data.domain.Page;

public record PageNumbersResult(Book book, Page<Integer> pageNumbers) {
}
