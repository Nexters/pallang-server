package com.nexters.palang.domain.book.application;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.presentation.dto.BookActivityListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookActivityResponse;
import com.nexters.palang.domain.book.presentation.dto.BookCarouselListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookResponse;
import com.nexters.palang.domain.book.presentation.dto.BookSearchListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookSearchResponse;
import com.nexters.palang.global.common.response.CarouselPageInfo;
import com.nexters.palang.global.common.response.PageInfo;
import java.util.List;
import org.springframework.data.domain.Page;

public final class BookMapper {

    private BookMapper() {
    }

    public static BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(), book.getTitle(), book.getAuthor(), book.getPublisher(),
                book.getPageCount(), book.getIsbn(), book.getCoverImageUrl(), book.getSource());
    }

    public static BookListResponse toListResponse(Page<Book> books) {
        return new BookListResponse(books.map(BookMapper::toResponse).getContent(), PageInfo.from(books));
    }

    public static BookSearchResponse toSearchResponse(BookSearchProjection projection) {
        return new BookSearchResponse(
                projection.bookId(), projection.title(), projection.author(), projection.publisher(),
                projection.pageCount(), projection.isbn(), projection.coverImageUrl(), projection.source(),
                projection.passageCount(), projection.opinionCount());
    }

    public static BookSearchListResponse toSearchListResponse(Page<BookSearchProjection> projections) {
        return new BookSearchListResponse(
                projections.map(BookMapper::toSearchResponse).getContent(), PageInfo.from(projections));
    }

    public static BookActivityResponse toActivityResponse(BookActivityProjection projection) {
        return new BookActivityResponse(
                projection.bookId(), projection.title(), projection.author(),
                projection.coverImageUrl(), projection.passageCount(), projection.opinionCount());
    }

    public static BookActivityListResponse toActivityListResponse(Page<BookActivityProjection> projections) {
        return new BookActivityListResponse(
                projections.map(BookMapper::toActivityResponse).getContent(), PageInfo.from(projections));
    }

    public static BookCarouselListResponse toCarouselListResponse(BookCarouselPage page) {
        List<BookActivityResponse> books = page.books().stream().map(BookMapper::toActivityResponse).toList();
        CarouselPageInfo pageInfo = CarouselPageInfo.of(page.offset(), page.size(), books.size(), page.totalElements());
        return new BookCarouselListResponse(books, pageInfo);
    }
}
