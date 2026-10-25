package com.nexters.palang.domain.book.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// 카카오 책 검색 응답의 documents 한 건. isbn은 "ISBN10 ISBN13"처럼 공백으로 구분돼 오고(둘 중
// 하나만 올 수도 있음), 저자는 배열로 온다. 정규화는 KakaoBookApiClient에서 한다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoBookDocument(
        String title,
        List<String> authors,
        String publisher,
        String isbn,
        String thumbnail
) {
}
