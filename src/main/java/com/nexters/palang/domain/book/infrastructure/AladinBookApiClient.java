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

@Component
public class AladinBookApiClient {

    private static final String VERSION = "20131101";

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

        if (response == null || response.item() == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        List<ExternalBookResult> content = response.item().stream().map(this::toExternalBookResult).toList();
        long totalResults = response.totalResults() != null ? response.totalResults() : content.size();
        return new PageImpl<>(content, pageable, totalResults);
    }

    private ExternalBookResult toExternalBookResult(AladinItem item) {
        return new ExternalBookResult(item.title(), item.author(), item.publisher(), item.isbn13(), item.cover());
    }
}
