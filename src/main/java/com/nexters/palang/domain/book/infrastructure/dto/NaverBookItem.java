package com.nexters.palang.domain.book.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 네이버 책 검색 응답의 items 한 건. isbn은 "ISBN10 ISBN13"처럼 공백으로 구분돼 오고(둘 중 하나만
// 올 수도 있음), author는 여러 명일 때 "^"로 구분된다. 정규화는 NaverBookApiClient에서 한다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverBookItem(
        String title,
        String author,
        String publisher,
        String isbn,
        String image
) {
}
