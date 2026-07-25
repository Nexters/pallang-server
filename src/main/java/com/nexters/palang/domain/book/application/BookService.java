package com.nexters.palang.domain.book.application;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.book.infrastructure.AladinBookApiClient;
import com.nexters.palang.domain.book.infrastructure.BookQueryRepository;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.book.presentation.dto.CreateBookRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private static final int RECENT_BOOKS_LIMIT = 10;
    private static final int POPULAR_BOOKS_LIMIT = 20;

    private final BookRepository bookRepository;
    private final BookQueryRepository bookQueryRepository;
    private final AladinBookApiClient aladinBookApiClient;

    public List<ExternalBookResult> searchExternalBooks(String keyword) {
        return aladinBookApiClient.search(keyword);
    }

    public List<Book> searchInternalBooks(String keyword) {
        return bookRepository.findByTitleContainingIgnoreCase(keyword);
    }

    @Transactional
    public Book createBook(CreateBookRequest request) {
        Book book = Book.builder()
                .title(request.title())
                .author(request.author())
                .publisher(request.publisher())
                .pageCount(request.pageCount())
                .isbn(request.isbn())
                .coverImageUrl(request.coverImageUrl())
                .source(BookSource.MANUAL)
                .build();
        return bookRepository.save(book);
    }

    public List<BookActivityProjection> getHomeCarouselBooks() {
        return bookQueryRepository.findCarouselBooks();
    }

    public List<Book> getRecentBooks(Long userId) {
        List<Long> bookIds = bookQueryRepository.findRecentlyActiveBookIds(userId, RECENT_BOOKS_LIMIT);
        Map<Long, Book> booksById = bookRepository.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, book -> book));
        return bookIds.stream().map(booksById::get).filter(Objects::nonNull).toList();
    }

    public List<BookActivityProjection> getPopularBooks() {
        return bookQueryRepository.findPopularBooks(POPULAR_BOOKS_LIMIT);
    }
}
