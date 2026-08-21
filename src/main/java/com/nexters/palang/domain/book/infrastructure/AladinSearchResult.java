package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.application.ExternalBookResult;
import java.util.List;

// AladinBookApiClient#search()의 캐싱 대상 반환 타입. Page/Pageable은 인터페이스라 Redis에
// JSON으로 캐싱했다가 역직렬화하기 까다로워서, 캐싱 가능한 단순 record로 결과(목록 + 총 개수)만 담는다.
public record AladinSearchResult(List<ExternalBookResult> items, long totalResults) {

    public static AladinSearchResult empty() {
        return new AladinSearchResult(List.of(), 0);
    }
}
