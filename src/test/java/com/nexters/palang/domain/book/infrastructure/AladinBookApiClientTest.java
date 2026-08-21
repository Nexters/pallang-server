package com.nexters.palang.domain.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.application.ExternalBookResult;
import com.nexters.palang.domain.book.common.error.BookException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AladinBookApiClientTest {

    @Mock
    private ExchangeFunction exchangeFunction;

    private AladinBookApiClient aladinBookApiClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://www.aladin.co.kr/ttb/api")
                .exchangeFunction(exchangeFunction)
                .build();
        return new AladinBookApiClient(webClient, "test-ttb-key");
    }

    private static ClientResponse jsonResponse(String json) {
        return ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(json)
                .build();
    }

    private void stubSearch(String searchJson) {
        given(exchangeFunction.exchange(any())).willReturn(Mono.just(jsonResponse(searchJson)));
    }

    // count개의 더미 도서 항목으로 이뤄진 알라딘 응답 JSON을 만든다. isbn은 idPrefix로 서로 구분되게 채운다.
    private static String itemsJson(int count, long totalResults, String idPrefix) {
        String items = IntStream.rangeClosed(1, count)
                .mapToObj(i -> """
                        { "title": "책%d", "author": "작가", "publisher": "출판사", "isbn13": "%s%d", "cover": "cover%d.jpg" }
                        """.formatted(i, idPrefix, i, i))
                .collect(Collectors.joining(","));
        return "{ \"totalResults\": %d, \"item\": [%s] }".formatted(totalResults, items);
    }

    @Test
    @DisplayName("검색 결과를 반환하며, 응답 속도를 위해 pageCount는 포함하지 않는다")
    void searchDoesNotEnrichPageCount() {
        stubSearch("""
                {
                  "totalResults": 1,
                  "item": [
                    {
                      "title": "프랑켄슈타인",
                      "author": "메리 셸리",
                      "publisher": "문학동네",
                      "isbn13": "9788954429721",
                      "cover": "https://image.aladin.co.kr/cover.jpg"
                    }
                  ]
                }
                """);

        AladinSearchResult result = aladinBookApiClient().searchAll("프랑켄슈타인");

        assertThat(result.items()).containsExactly(
                new ExternalBookResult("프랑켄슈타인", "메리 셸리", "문학동네", "9788954429721",
                        "https://image.aladin.co.kr/cover.jpg"));
        assertThat(result.totalResults()).isEqualTo(1);
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 결과를 반환한다")
    void searchReturnsEmptyResultWhenNoItems() {
        stubSearch("{}");

        AladinSearchResult result = aladinBookApiClient().searchAll("없는 책");

        assertThat(result.items()).isEmpty();
        assertThat(result.totalResults()).isZero();
    }

    @Test
    @DisplayName("알라딘 검색 요청이 실패하면 BookException을 던진다")
    void searchThrowsBookExceptionWhenAladinFails() {
        given(exchangeFunction.exchange(any())).willReturn(
                Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).body("error").build()));

        assertThatThrownBy(() -> aladinBookApiClient().searchAll("프랑켄슈타인"))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("1페이지를 알라딘의 1부터 시작하는 start 파라미터와 최대 MaxResults(100)로 요청한다")
    void searchAllRequestsFirstPageWithMaxAllowedResults() {
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        given(exchangeFunction.exchange(requestCaptor.capture())).willReturn(Mono.just(jsonResponse("{}")));

        aladinBookApiClient().searchAll("프랑켄슈타인");

        String query = requestCaptor.getValue().url().getQuery();
        assertThat(query).contains("start=1").contains("MaxResults=100");
    }

    @Test
    @DisplayName("고화질 커버 이미지를 받기 위해 Cover=Big 파라미터를 함께 요청한다")
    void searchRequestsBigCoverImage() {
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        given(exchangeFunction.exchange(requestCaptor.capture())).willReturn(Mono.just(jsonResponse("{}")));

        aladinBookApiClient().searchAll("프랑켄슈타인");

        String query = requestCaptor.getValue().url().getQuery();
        assertThat(query).contains("Cover=Big");
    }

    @Test
    @DisplayName("1페이지가 MaxResults(100)만큼 꽉 차서 오면 다음 페이지(start=2)를 이어서 요청해 합친다")
    void searchAllFetchesNextPageWhenFirstPageIsFull() {
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        given(exchangeFunction.exchange(requestCaptor.capture())).willReturn(
                Mono.just(jsonResponse(itemsJson(100, 130, "first-"))),
                Mono.just(jsonResponse(itemsJson(30, 130, "second-"))));

        AladinSearchResult result = aladinBookApiClient().searchAll("책모음");

        assertThat(result.items()).hasSize(130);
        assertThat(result.totalResults()).isEqualTo(130);
        List<String> queries = requestCaptor.getAllValues().stream()
                .map(request -> request.url().getQuery())
                .toList();
        assertThat(queries.get(0)).contains("start=1").contains("MaxResults=100");
        assertThat(queries.get(1)).contains("start=2").contains("MaxResults=100");
        verify(exchangeFunction, times(2)).exchange(any());
    }

    @Test
    @DisplayName("결과가 알라딘 최대 제공량(200건)을 넘어가면 더 요청하지 않고 200건까지만 모은다")
    void searchAllStopsAtAladinMaxTotalResultsCap() {
        // 매번 100건씩 꽉 채워서 응답하면(끝없이 더 있는 것처럼) 무한 루프를 도는 대신 200건에서 멈춰야 한다.
        given(exchangeFunction.exchange(any())).willReturn(
                Mono.just(jsonResponse(itemsJson(100, 9999, "a-"))),
                Mono.just(jsonResponse(itemsJson(100, 9999, "b-"))));

        AladinSearchResult result = aladinBookApiClient().searchAll("인기키워드");

        assertThat(result.items()).hasSize(200);
        verify(exchangeFunction, times(2)).exchange(any());
    }
}
