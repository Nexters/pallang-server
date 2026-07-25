package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.application.ExternalBookResult;
import com.nexters.palang.domain.book.infrastructure.dto.AladinItem;
import com.nexters.palang.domain.book.infrastructure.dto.AladinItemSearchResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class AladinBookApiClient {

    private static final String VERSION = "20131101";
    // ItemSearch는 페이지수를 주지 않아 결과마다 ItemLookUp을 추가 호출해야 한다.
    // 알라딘 일일 호출 한도를 한 번에 너무 많이 소진하지 않도록 동시 호출 수를 제한한다.
    private static final int PAGE_COUNT_LOOKUP_CONCURRENCY = 5;

    private final WebClient aladinWebClient;
    private final String ttbKey;

    public AladinBookApiClient(WebClient aladinWebClient, @Value("${aladin.ttb-key}") String ttbKey) {
        this.aladinWebClient = aladinWebClient;
        this.ttbKey = ttbKey;
    }

    public Page<ExternalBookResult> search(String keyword, Pageable pageable) {
        // 알라딘 start는 1부터 시작하는 인덱스
        int start = (int) pageable.getOffset() + 1;
        int maxResults = pageable.getPageSize();

        AladinItemSearchResponse response = aladinWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ItemSearch.aspx")
                        .queryParam("ttbkey", ttbKey)
                        .queryParam("Query", keyword)
                        .queryParam("QueryType", "Title")
                        .queryParam("MaxResults", maxResults)
                        .queryParam("start", start)
                        .queryParam("SearchTarget", "Book")
                        .queryParam("output", "js")
                        .queryParam("Version", VERSION)
                        .build())
                .retrieve()
                .bodyToMono(AladinItemSearchResponse.class)
                .block();

        if (response == null || response.item() == null || response.item().isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<ExternalBookResult> content = Flux.fromIterable(response.item())
                .flatMap(this::toExternalBookResultWithPageCount, PAGE_COUNT_LOOKUP_CONCURRENCY)
                .collectList()
                .block();

        long totalResults = response.totalResults() != null ? response.totalResults() : content.size();
        return new PageImpl<>(content, pageable, totalResults);
    }

    private Mono<ExternalBookResult> toExternalBookResultWithPageCount(AladinItem item) {
        if (item.isbn13() == null || item.isbn13().isBlank()) {
            return Mono.just(toExternalBookResult(item, null));
        }
        return lookupPageCount(item.isbn13())
                .map(pageCount -> toExternalBookResult(item, pageCount))
                .onErrorReturn(toExternalBookResult(item, null));
    }

    private Mono<Integer> lookupPageCount(String isbn13) {
        return aladinWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ItemLookUp.aspx")
                        .queryParam("ttbkey", ttbKey)
                        .queryParam("ItemId", isbn13)
                        .queryParam("ItemIdType", "ISBN13")
                        .queryParam("output", "js")
                        .queryParam("Version", VERSION)
                        .queryParam("OptResult", "itemPage")
                        .build())
                .retrieve()
                .bodyToMono(AladinItemSearchResponse.class)
                .map(this::extractPageCount);
    }

    private Integer extractPageCount(AladinItemSearchResponse response) {
        if (response.item() == null || response.item().isEmpty()) {
            return null;
        }
        var subInfo = response.item().get(0).subInfo();
        return subInfo != null ? subInfo.itemPage() : null;
    }

    private ExternalBookResult toExternalBookResult(AladinItem item, Integer pageCount) {
        return new ExternalBookResult(item.title(), item.author(), item.publisher(), pageCount, item.isbn13(), item.cover());
    }
}
