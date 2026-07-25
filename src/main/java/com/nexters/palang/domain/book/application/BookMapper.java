package com.nexters.palang.domain.book.application;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.presentation.dto.BookActivityListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookActivityResponse;
import com.nexters.palang.domain.book.presentation.dto.BookListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookResponse;
import com.nexters.palang.domain.book.presentation.dto.ExternalBookListResponse;
import com.nexters.palang.domain.book.presentation.dto.ExternalBookResponse;
import java.util.List;

public final class BookMapper {

    private BookMapper() {
    }

    public static BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(), book.getTitle(), book.getAuthor(), book.getPublisher(),
                book.getPageCount(), book.getIsbn(), book.getCoverImageUrl(), book.getSource());
    }

    public static BookListResponse toListResponse(List<Book> books) {
        return new BookListResponse(books.stream().map(BookMapper::toResponse).toList());
    }

    public static ExternalBookResponse toExternalResponse(ExternalBookResult result) {
        return new ExternalBookResponse(
                result.title(), result.author(), result.publisher(),
                result.pageCount(), result.isbn(), result.coverImageUrl());
    }

    public static ExternalBookListResponse toExternalListResponse(List<ExternalBookResult> results) {
        return new ExternalBookListResponse(results.stream().map(BookMapper::toExternalResponse).toList());
    }

    public static BookActivityResponse toActivityResponse(BookActivityProjection projection) {
        return new BookActivityResponse(
                projection.bookId(), projection.title(), projection.author(),
                projection.coverImageUrl(), projection.passageCount(), projection.opinionCount());
    }

    public static BookActivityListResponse toActivityListResponse(List<BookActivityProjection> projections) {
        return new BookActivityListResponse(projections.stream().map(BookMapper::toActivityResponse).toList());
    }
}
