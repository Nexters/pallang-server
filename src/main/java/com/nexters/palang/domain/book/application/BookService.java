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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final BookQueryRepository bookQueryRepository;
    private final AladinBookApiClient aladinBookApiClient;

    // DB를 사용하지 않고 알라딘 API 호출(최대 수 초)만 하므로, DB 커넥션을 불필요하게 오래 붙잡지 않도록 트랜잭션에서 제외한다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Page<ExternalBookResult> searchExternalBooks(String keyword, Pageable pageable) {
        return aladinBookApiClient.search(keyword, pageable);
    }

    public Page<BookSearchProjection> searchInternalBooks(String keyword, Pageable pageable) {
        return bookQueryRepository.searchByTitle(keyword, pageable);
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

    // offset을 지정하지 않으면 전체 목록 중 가운데 책들을 반환한다. 좌우 스크롤 시에는 이전/다음 offset을 그대로 넘기면 된다.
    public BookCarouselPage getHomeCarouselBooks(Long offset, int size) {
        long total = bookQueryRepository.countCarouselBooks();
        long resolvedOffset = offset != null ? Math.max(0, offset) : centerOffset(total, size);
        List<BookActivityProjection> books = bookQueryRepository.findCarouselBooks(resolvedOffset, size);
        return new BookCarouselPage(books, resolvedOffset, size, total);
    }

    private long centerOffset(long total, int size) {
        return Math.max(0, (total - size) / 2);
    }

    public Page<Book> getRecentBooks(Long userId, Pageable pageable) {
        Page<Long> bookIds = bookQueryRepository.findRecentlyActiveBookIds(userId, pageable);
        Map<Long, Book> booksById = bookRepository.findAllById(bookIds.getContent()).stream()
                .collect(Collectors.toMap(Book::getId, book -> book));
        List<Book> books = bookIds.getContent().stream().map(booksById::get).filter(Objects::nonNull).toList();
        return new PageImpl<>(books, pageable, bookIds.getTotalElements());
    }

    public Page<BookActivityProjection> getPopularBooks(Pageable pageable) {
        return bookQueryRepository.findPopularBooks(pageable);
    }
}
