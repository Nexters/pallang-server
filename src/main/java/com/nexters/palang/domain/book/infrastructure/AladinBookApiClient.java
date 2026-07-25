package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.application.ExternalBookResult;
import com.nexters.palang.domain.book.infrastructure.dto.AladinItem;
import com.nexters.palang.domain.book.infrastructure.dto.AladinItemSearchResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AladinBookApiClient {

    private static final String VERSION = "20131101";
    private static final int MAX_RESULTS = 20;

    private final WebClient aladinWebClient;
    private final String ttbKey;

    public AladinBookApiClient(WebClient aladinWebClient, @Value("${aladin.ttb-key}") String ttbKey) {
        this.aladinWebClient = aladinWebClient;
        this.ttbKey = ttbKey;
    }

    public List<ExternalBookResult> search(String keyword) {
        AladinItemSearchResponse response = aladinWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ItemSearch.aspx")
                        .queryParam("ttbkey", ttbKey)
                        .queryParam("Query", keyword)
                        .queryParam("QueryType", "Title")
                        .queryParam("MaxResults", MAX_RESULTS)
                        .queryParam("start", 1)
                        .queryParam("SearchTarget", "Book")
                        .queryParam("output", "js")
                        .queryParam("Version", VERSION)
                        .build())
                .retrieve()
                .bodyToMono(AladinItemSearchResponse.class)
                .block();

        if (response == null || response.item() == null) {
            return List.of();
        }
        return response.item().stream().map(this::toExternalBookResult).toList();
    }

    private ExternalBookResult toExternalBookResult(AladinItem item) {
        return new ExternalBookResult(item.title(), item.author(), item.publisher(), item.isbn13(), item.cover());
    }
}
