package com.nexters.palang.domain.group.application;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.presentation.dto.GroupBookListResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupBookResponse;
import com.nexters.palang.global.common.response.PageInfo;

public final class GroupBookMapper {

    private GroupBookMapper() {
    }

    public static GroupBookResponse toResponse(GroupBookView view) {
        GroupBookEntry entry = view.entry();
        Book book = entry.getBook();
        return new GroupBookResponse(
                entry.getId(), book.getId(), book.getTitle(), book.getAuthor(), book.getCoverImageUrl(),
                entry.getStartDate(), entry.getEndDate(), view.status(), entry.getStatusMode(),
                view.passageCount(), view.opinionCount(), entry.getCreatedAt());
    }

    public static GroupBookListResponse toListResponse(GroupBookPage page) {
        return new GroupBookListResponse(
                page.statusFilter(), page.readingCount(), page.finishedCount(),
                page.books().map(GroupBookMapper::toResponse).getContent(), PageInfo.from(page.books()));
    }
}
