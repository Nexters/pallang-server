package com.nexters.palang.domain.book.presentation;

import com.nexters.palang.domain.book.application.BookMapper;
import com.nexters.palang.domain.book.application.BookService;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.presentation.dto.BookActivityListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookResponse;
import com.nexters.palang.domain.book.presentation.dto.CreateBookRequest;
import com.nexters.palang.domain.book.presentation.dto.ExternalBookListResponse;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookController implements BookApi {

    private final BookService bookService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @GetMapping("/api/books/search")
    public ResponseEntity<DataResponse<ExternalBookListResponse>> searchExternalBooks(@RequestParam String keyword) {
        return ResponseEntity.ok(DataResponse.from(
                BookMapper.toExternalListResponse(bookService.searchExternalBooks(keyword))));
    }

    @Override
    @GetMapping("/api/books/internal-search")
    public ResponseEntity<DataResponse<BookListResponse>> searchInternalBooks(@RequestParam String keyword) {
        return ResponseEntity.ok(DataResponse.from(
                BookMapper.toListResponse(bookService.searchInternalBooks(keyword))));
    }

    @Override
    @PostMapping("/api/books")
    public ResponseEntity<DataResponse<BookResponse>> createBook(@RequestBody @Valid CreateBookRequest request) {
        Book book = bookService.createBook(request);
        return ResponseEntity.ok(DataResponse.from(BookMapper.toResponse(book)));
    }

    @Override
    @GetMapping("/api/home/books")
    public ResponseEntity<DataResponse<BookActivityListResponse>> getHomeCarouselBooks() {
        return ResponseEntity.ok(DataResponse.from(
                BookMapper.toActivityListResponse(bookService.getHomeCarouselBooks())));
    }

    @Override
    @GetMapping("/api/books/recent")
    public ResponseEntity<DataResponse<BookListResponse>> getRecentBooks() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(
                BookMapper.toListResponse(bookService.getRecentBooks(currentUserId))));
    }

    @Override
    @GetMapping("/api/books/popular")
    public ResponseEntity<DataResponse<BookActivityListResponse>> getPopularBooks() {
        return ResponseEntity.ok(DataResponse.from(
                BookMapper.toActivityListResponse(bookService.getPopularBooks())));
    }
}
