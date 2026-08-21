package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.application.ExternalBookResult;
import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.infrastructure.dto.AladinItem;
import com.nexters.palang.domain.book.infrastructure.dto.AladinItemSearchResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AladinBookApiClient {

    private static final String VERSION = "20131101";
    // 알라딘 ItemSearch.aspx의 한도: MaxResults는 한 번 요청에 최대 100건, 키워드당 전체 결과는
    // 최대 200건까지만 내려준다(그 이상은 알라딘 자체가 지원하지 않음).
    private static final int MAX_RESULTS_PER_CALL = 100;
    private static final int MAX_TOTAL_RESULTS = 200;

    private final WebClient aladinWebClient;
    private final String ttbKey;

    public AladinBookApiClient(WebClient aladinWebClient, @Value("${aladin.ttb-key}") String ttbKey) {
        this.aladinWebClient = aladinWebClient;
        this.ttbKey = ttbKey;
    }

    // 결과마다 ItemLookUp을 추가 호출해 페이지수를 채우던 방식은 검색 1회당 응답이 수 초씩 걸려 제거했다.
    // pageCount는 검색 응답에서 아예 제공하지 않으며, 필요하면 도서 상세 조회 시점에 별도로 채우는 것을 후속 과제로 검토한다.
    //
    // 알라딘 ItemSearch의 start 파라미터는 레코드 오프셋이 아니라 "몇 번째 페이지냐"이고(MaxResults
    // 단위로만 나뉨), 임의의 레코드 위치를 직접 요청할 방법이 없다. GET /api/books/search는 DB 검색
    // 결과와 하나로 이어붙인 목록처럼 페이지네이션하면서 page 경계에 맞지 않는 임의 offset이 필요하므로,
    // 여기서는 아예 키워드당 결과 전체(최대 200건, 알라딘 한도)를 미리 다 모아서 반환하고, 실제
    // offset/limit 슬라이싱은 호출부(BookService)가 이 리스트를 대상으로 메모리에서 처리한다.
    //
    // 자동완성이 타이핑마다 이 메서드를 호출하므로 @Cacheable로 캐싱한다 (키: keyword, TTL은
    // CacheConfig 참고). 실패(예외)는 Spring 캐시 추상화가 애초에 캐싱하지 않고, 빈 결과는 unless로
    // 캐싱에서 제외해 다음 호출에서 다시 시도하게 한다.
    // 이 메서드는 반드시 다른 빈(BookService)이 호출해야 한다 — 같은 클래스 안에서 self-invocation하면
    // Spring AOP 프록시를 거치지 않아 캐싱이 동작하지 않는다.
    @Cacheable(value = "aladinBookSearch", key = "#keyword", unless = "#result.items().isEmpty()")
    public AladinSearchResult searchAll(String keyword) {
        List<ExternalBookResult> items = new ArrayList<>();
        long totalResults = 0;

        // page는 알라딘 start 파라미터(1부터 시작하는 페이지 번호)와 동일하다.
        for (int page = 1; items.size() < MAX_TOTAL_RESULTS; page++) {
            AladinItemSearchResponse response = fetchPage(keyword, page);
            if (response == null || response.item() == null || response.item().isEmpty()) {
                break;
            }

            totalResults = response.totalResults() != null ? response.totalResults() : items.size() + response.item().size();
            items.addAll(response.item().stream().map(this::toExternalBookResult).toList());

            // 이번 페이지가 MAX_RESULTS_PER_CALL보다 적게 왔다면 알라딘이 가진 결과를 이미 다 받은 것이다.
            if (response.item().size() < MAX_RESULTS_PER_CALL) {
                break;
            }
        }

        if (items.isEmpty()) {
            return AladinSearchResult.empty();
        }
        return new AladinSearchResult(items, totalResults);
    }

    private AladinItemSearchResponse fetchPage(String keyword, int page) {
        try {
            return aladinWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ItemSearch.aspx")
                            .queryParam("ttbkey", ttbKey)
                            .queryParam("Query", keyword)
                            .queryParam("QueryType", "Title")
                            .queryParam("MaxResults", MAX_RESULTS_PER_CALL)
                            .queryParam("start", page)
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
    }

    private ExternalBookResult toExternalBookResult(AladinItem item) {
        return new ExternalBookResult(item.title(), item.author(), item.publisher(), item.isbn13(), item.cover());
    }
}
