package com.nexters.palang.domain.passage.application;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.passage.domain.Passage;

// 이미지 저장(꾸미기) 화면의 프레임에 들어갈 값: 발췌 문장, 책 제목, 작가명, 발췌 페이지만 담는다.
// 의견·댓글은 이미지에서 제외한다(기획). open-in-view: false라 book(LAZY)을 트랜잭션 안에서 꺼내 둔다.
public record PassageImageCardResult(
        Long passageId,
        String quotedText,
        int pageNumber,
        String bookTitle,
        String bookAuthor
) {
    public static PassageImageCardResult from(Passage passage) {
        Book book = passage.getBook();
        return new PassageImageCardResult(
                passage.getId(), passage.getQuotedText(), passage.getPageNumber(), book.getTitle(), book.getAuthor());
    }
}
