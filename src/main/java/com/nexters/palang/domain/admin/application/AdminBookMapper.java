package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.admin.presentation.dto.AdminBookListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminBookSummaryResponse;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.global.common.response.PageInfo;
import org.springframework.data.domain.Page;

public final class AdminBookMapper {

    private AdminBookMapper() {
    }

    public static AdminBookSummaryResponse toSummary(Book book) {
        return new AdminBookSummaryResponse(
                book.getId(), book.getTitle(), book.getAuthor(), book.getPublisher(), book.getPageCount(),
                book.getIsbn(), book.getCoverImageUrl(), book.getSource().name(), book.getCreatedAt());
    }

    public static AdminBookListResponse toListResponse(Page<Book> books) {
        return new AdminBookListResponse(books.map(AdminBookMapper::toSummary).getContent(), PageInfo.from(books));
    }
}
