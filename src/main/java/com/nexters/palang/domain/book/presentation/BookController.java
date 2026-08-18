package com.nexters.palang.domain.book.presentation;

import com.nexters.palang.domain.book.application.BookMapper;
import com.nexters.palang.domain.book.application.BookSearchSort;
import com.nexters.palang.domain.book.application.BookService;
import com.nexters.palang.domain.book.application.OpinionCountScope;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.presentation.dto.BookActivityListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookCarouselListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookDetailResponse;
import com.nexters.palang.domain.book.presentation.dto.BookListResponse;
import com.nexters.palang.domain.book.presentation.dto.BookResponse;
import com.nexters.palang.domain.book.presentation.dto.BookSearchListResponse;
import com.nexters.palang.domain.book.presentation.dto.CreateBookRequest;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class BookController implements BookApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final BookService bookService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @GetMapping("/api/books/search")
    public ResponseEntity<DataResponse<BookSearchListResponse>> searchBooks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(DataResponse.from(
                BookMapper.toSearchListResponse(bookService.searchBooks(keyword, pageable(page, size)))));
    }

    @Override
    @GetMapping("/api/books/internal-search")
    public ResponseEntity<DataResponse<BookSearchListResponse>> searchInternalBooks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "RECENT") BookSearchSort sort,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(DataResponse.from(BookMapper.toSearchListResponse(
                bookService.searchInternalBooks(keyword, sort, pageable(page, size)))));
    }

    @Override
    @PostMapping(path = "/api/books", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<BookResponse>> createBook(
            @RequestPart("book") @Valid CreateBookRequest request,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage) {
        Book book = bookService.createBook(request, coverImage);
        return ResponseEntity.ok(DataResponse.from(BookMapper.toResponse(book)));
    }

    @Override
    @GetMapping("/api/home/books")
    public ResponseEntity<DataResponse<BookCarouselListResponse>> getHomeCarouselBooks(
            @RequestParam(required = false) Long offset,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        int clampedSize = Math.clamp(size, 1, MAX_SIZE);
        return ResponseEntity.ok(DataResponse.from(
                BookMapper.toCarouselListResponse(bookService.getHomeCarouselBooks(offset, clampedSize))));
    }

    @Override
    @GetMapping("/api/books/my-library")
    public ResponseEntity<DataResponse<BookActivityListResponse>> getMyLibraryBooks(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = "ALL") OpinionCountScope opinionCountScope) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(BookMapper.toActivityListResponse(
                bookService.getMyLibraryBooks(currentUserId, pageable(page, size), opinionCountScope))));
    }

    @Override
    @GetMapping("/api/books/recent")
    public ResponseEntity<DataResponse<BookListResponse>> getRecentBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(BookMapper.toListResponse(
                bookService.getRecentBooks(currentUserId, keyword, pageable(page, size)))));
    }

    @Override
    @GetMapping("/api/books/popular")
    public ResponseEntity<DataResponse<BookActivityListResponse>> getPopularBooks(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(DataResponse.from(
                BookMapper.toActivityListResponse(bookService.getPopularBooks(pageable(page, size)))));
    }

    @Override
    @GetMapping("/api/books/{bookId}")
    public ResponseEntity<DataResponse<BookDetailResponse>> getBookDetail(@PathVariable Long bookId) {
        Long currentUserId = currentUserProvider.findCurrentUserId().orElse(null);
        return ResponseEntity.ok(DataResponse.from(
                BookMapper.toDetailResponse(bookService.getBookDetail(bookId, currentUserId))));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
