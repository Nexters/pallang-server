package com.nexters.palang.domain.admin.presentation;

import com.nexters.palang.domain.admin.application.AdminAccessGuard;
import com.nexters.palang.domain.admin.application.AdminBookMapper;
import com.nexters.palang.domain.admin.application.AdminBookService;
import com.nexters.palang.domain.admin.presentation.dto.AdminBookListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminBookSummaryResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminUpdateBookRequest;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.global.common.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminBookController implements AdminBookApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AdminAccessGuard adminAccessGuard;
    private final AdminBookService adminBookService;

    @Override
    @GetMapping("/api/admin/books")
    public ResponseEntity<DataResponse<AdminBookListResponse>> searchBooks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        adminAccessGuard.requireAdmin();
        Page<Book> results = adminBookService.searchBooks(keyword, pageable(page, size));
        return ResponseEntity.ok(DataResponse.from(AdminBookMapper.toListResponse(results)));
    }

    @Override
    @PatchMapping("/api/admin/books/{bookId}")
    public ResponseEntity<DataResponse<AdminBookSummaryResponse>> updateBook(
            @PathVariable Long bookId, @Valid @RequestBody AdminUpdateBookRequest request) {
        adminAccessGuard.requireAdmin();
        Book book = adminBookService.updateBook(bookId, request.title(), request.author(), request.publisher(),
                request.pageCount(), request.isbn(), request.coverImageUrl());
        return ResponseEntity.ok(DataResponse.from(AdminBookMapper.toSummary(book)));
    }

    @Override
    @DeleteMapping("/api/admin/books/{bookId}")
    public ResponseEntity<DataResponse<Void>> deleteBook(@PathVariable Long bookId) {
        adminAccessGuard.requireAdmin();
        adminBookService.deleteBook(bookId);
        return ResponseEntity.ok(DataResponse.from(null));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
