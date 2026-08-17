package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.application.ExternalBookResult;
import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.infrastructure.dto.AladinItem;
import com.nexters.palang.domain.book.infrastructure.dto.AladinItemSearchResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AladinBookApiClient {

    private static final String VERSION = "20131101";

    private final WebClient aladinWebClient;
    private final String ttbKey;

    public AladinBookApiClient(WebClient aladinWebClient, @Value("${aladin.ttb-key}") String ttbKey) {
        this.aladinWebClient = aladinWebClient;
        this.ttbKey = ttbKey;
    }

    // 결과마다 ItemLookUp을 추가 호출해 페이지수를 채우던 방식은 검색 1회당 응답이 수 초씩 걸려 제거했다.
    // pageCount는 검색 응답에서 아예 제공하지 않으며, 필요하면 도서 상세 조회 시점에 별도로 채우는 것을 후속 과제로 검토한다.
    //
    // 자동완성이 타이핑마다 이 메서드를 호출하므로 @Cacheable로 캐싱한다 (키: keyword+offset+maxResults,
    // TTL은 CacheConfig 참고). 실패(예외)는 Spring 캐시 추상화가 애초에 캐싱하지 않고, 빈 결과는 unless로
    // 캐싱에서 제외해 다음 호출에서 다시 시도하게 한다.
    // 이 메서드는 반드시 다른 빈(BookService)이 호출해야 한다 — 같은 클래스 안에서 self-invocation하면
    // Spring AOP 프록시를 거치지 않아 캐싱이 동작하지 않는다.
    // Pageable 대신 offset/maxResults를 직접 받는다. GET /api/books/search가 DB 검색 결과와 하나의
    // 연속된 목록처럼 페이지네이션하면서 page*size로는 표현할 수 없는 임의의 offset을 요청해야 하기
    // 때문이다(findCarouselBooks와 동일한 이유).
    @Cacheable(value = "aladinBookSearch", key = "#keyword + ':' + #offset + ':' + #maxResults",
            unless = "#result.items().isEmpty()")
    public AladinSearchResult search(String keyword, int offset, int maxResults) {
        // 알라딘 start는 1부터 시작하는 인덱스
        int start = offset + 1;

        AladinItemSearchResponse response;
        try {
            response = aladinWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ItemSearch.aspx")
                            .queryParam("ttbkey", ttbKey)
                            .queryParam("Query", keyword)
                            .queryParam("QueryType", "Title")
                            .queryParam("MaxResults", maxResults)
                            .queryParam("start", start)
                            .queryParam("SearchTarget", "Book")
                            .queryParam("Cover", "Big")
                            .queryParam("output", "js")
                            .queryParam("Version", VERSION)
                            .build())
                    .retrieve()
                    .bodyToMono(AladinItemSearchResponse.class)
                    .block();
        } catch (RuntimeException e) {
            throw new BookException(BookErrorCode.EXTERNAL_SEARCH_FAILED, e);
        }

        if (response == null || response.item() == null || response.item().isEmpty()) {
            return AladinSearchResult.empty();
        }

        List<ExternalBookResult> content = response.item().stream().map(this::toExternalBookResult).toList();
        long totalResults = response.totalResults() != null ? response.totalResults() : content.size();
        return new AladinSearchResult(content, totalResults);
    }

    private ExternalBookResult toExternalBookResult(AladinItem item) {
        return new ExternalBookResult(item.title(), item.author(), item.publisher(), item.isbn13(), item.cover());
    }
}
