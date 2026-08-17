package com.nexters.palang.domain.book.application;

import com.nexters.palang.domain.book.domain.BookSource;

public record BookSearchProjection(
        Long bookId,
        String title,
        String author,
        String publisher,
        int pageCount,
        String isbn,
        String coverImageUrl,
        BookSource source,
        long passageCount,
        long opinionCount
) {

    // 알라딘에만 있고 아직 서비스 DB에 등록되지 않은 도서는 bookId/source가 없고, pageCount/passageCount/
    // opinionCount는 0으로 채운다. 클라이언트는 bookId 유무로 "이미 등록된 도서"와 "미등록 도서"를 구분한다.
    public static BookSearchProjection from(ExternalBookResult result) {
        return new BookSearchProjection(
                null, result.title(), result.author(), result.publisher(), 0,
                result.isbn(), result.coverImageUrl(), null, 0L, 0L);
    }
}
