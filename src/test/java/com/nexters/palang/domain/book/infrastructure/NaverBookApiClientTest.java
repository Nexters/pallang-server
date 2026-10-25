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
class NaverBookApiClientTest {

    @Mock
    private ExchangeFunction exchangeFunction;

    private NaverBookApiClient naverBookApiClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://openapi.naver.com")
                .exchangeFunction(exchangeFunction)
                .build();
        return new NaverBookApiClient(webClient, "test-client-id", "test-client-secret");
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

    // count개의 더미 도서 항목으로 이뤄진 네이버 응답 JSON을 만든다. isbn은 idPrefix로 서로 구분되게 채운다.
    private static String itemsJson(int count, long total, String idPrefix) {
        String items = IntStream.rangeClosed(1, count)
                .mapToObj(i -> """
                        { "title": "책%d", "author": "작가", "publisher": "출판사", "isbn": "%s%d", "image": "cover%d.jpg" }
                        """.formatted(i, idPrefix, i, i))
                .collect(Collectors.joining(","));
        return "{ \"total\": %d, \"items\": [%s] }".formatted(total, items);
    }

    @Test
    @DisplayName("검색 결과를 ExternalBookResult로 변환하며, ISBN은 13자리 값을 고른다")
    void searchMapsItemsAndPicksIsbn13() {
        stubSearch("""
                {
                  "total": 1,
                  "items": [
                    {
                      "title": "프랑켄슈타인",
                      "author": "메리 셸리",
                      "publisher": "문학동네",
                      "isbn": "8954429726 9788954429721",
                      "image": "https://shopping-phinf.pstatic.net/cover.jpg"
                    }
                  ]
                }
                """);

        ExternalBookSearchResult result = naverBookApiClient().searchAll("프랑켄슈타인");

        assertThat(result.items()).containsExactly(
                new ExternalBookResult("프랑켄슈타인", "메리 셸리", "문학동네", "9788954429721",
                        "https://shopping-phinf.pstatic.net/cover.jpg"));
        assertThat(result.totalResults()).isEqualTo(1);
    }

    @Test
    @DisplayName("제목의 강조 태그와 HTML 엔티티를 제거하고, ^로 구분된 저자를 쉼표로 이어붙인다")
    void searchCleansHtmlAndJoinsAuthors() {
        stubSearch("""
                {
                  "total": 1,
                  "items": [
                    { "title": "<b>죄와</b> 벌 &amp; 기타", "author": "도스토옙스키^김연경", "publisher": "민음사",
                      "isbn": "9788937461033", "image": "" }
                  ]
                }
                """);

        ExternalBookSearchResult result = naverBookApiClient().searchAll("죄와");

        assertThat(result.items()).containsExactly(
                new ExternalBookResult("죄와 벌 & 기타", "도스토옙스키, 김연경", "민음사", "9788937461033", null));
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 결과를 반환한다")
    void searchReturnsEmptyResultWhenNoItems() {
        stubSearch("{ \"total\": 0, \"items\": [] }");

        ExternalBookSearchResult result = naverBookApiClient().searchAll("없는 책");

        assertThat(result.items()).isEmpty();
        assertThat(result.totalResults()).isZero();
    }

    @Test
    @DisplayName("네이버 검색 요청이 실패하면 BookException을 던진다")
    void searchThrowsBookExceptionWhenNaverFails() {
        given(exchangeFunction.exchange(any())).willReturn(
                Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).body("error").build()));

        assertThatThrownBy(() -> naverBookApiClient().searchAll("프랑켄슈타인"))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("클라이언트 ID/Secret을 헤더로 보내고, start=1, display=100으로 첫 요청을 보낸다")
    void searchSendsCredentialHeadersAndFirstPageParams() {
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        given(exchangeFunction.exchange(requestCaptor.capture())).willReturn(Mono.just(jsonResponse("{}")));

        naverBookApiClient().searchAll("프랑켄슈타인");

        ClientRequest request = requestCaptor.getValue();
        assertThat(request.url().getPath()).isEqualTo("/v1/search/book.json");
        assertThat(request.url().getQuery()).contains("start=1").contains("display=100");
        assertThat(request.headers().getFirst("X-Naver-Client-Id")).isEqualTo("test-client-id");
        assertThat(request.headers().getFirst("X-Naver-Client-Secret")).isEqualTo("test-client-secret");
    }

    @Test
    @DisplayName("첫 요청이 display(100)만큼 꽉 차서 오면 start=101로 이어서 요청해 합친다")
    void searchAllFetchesNextPageWhenFirstPageIsFull() {
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        given(exchangeFunction.exchange(requestCaptor.capture())).willReturn(
                Mono.just(jsonResponse(itemsJson(100, 130, "first-"))),
                Mono.just(jsonResponse(itemsJson(30, 130, "second-"))));

        ExternalBookSearchResult result = naverBookApiClient().searchAll("책모음");

        assertThat(result.items()).hasSize(130);
        assertThat(result.totalResults()).isEqualTo(130);
        List<String> queries = requestCaptor.getAllValues().stream()
                .map(request -> request.url().getQuery())
                .toList();
        assertThat(queries.get(0)).contains("start=1").contains("display=100");
        assertThat(queries.get(1)).contains("start=101").contains("display=100");
        verify(exchangeFunction, times(2)).exchange(any());
    }

    @Test
    @DisplayName("결과가 200건을 넘어가면 더 요청하지 않고 200건까지만 모은다")
    void searchAllStopsAtMaxTotalResultsCap() {
        given(exchangeFunction.exchange(any())).willReturn(
                Mono.just(jsonResponse(itemsJson(100, 9999, "a-"))),
                Mono.just(jsonResponse(itemsJson(100, 9999, "b-"))));

        ExternalBookSearchResult result = naverBookApiClient().searchAll("인기키워드");

        assertThat(result.items()).hasSize(200);
        verify(exchangeFunction, times(2)).exchange(any());
    }

    @Test
    @DisplayName("ISBN으로 검색해 ISBN이 정확히 일치하는 도서만 반환한다")
    void findByIsbnReturnsOnlyExactIsbnMatch() {
        stubSearch("""
                {
                  "total": 2,
                  "items": [
                    { "title": "다른 책", "isbn": "9788936433000", "image": "other.jpg" },
                    { "title": "채식주의자", "isbn": "8936433598 9788936433598", "image": "naver.jpg" }
                  ]
                }
                """);

        assertThat(naverBookApiClient().findByIsbn("9788936433598"))
                .map(ExternalBookResult::coverImageUrl)
                .contains("naver.jpg");
    }

    @Test
    @DisplayName("ISBN이 일치하는 도서가 없으면 빈 Optional을 반환한다")
    void findByIsbnReturnsEmptyWhenNoExactMatch() {
        stubSearch("""
                { "total": 1, "items": [ { "title": "다른 책", "isbn": "9788936433000", "image": "other.jpg" } ] }
                """);

        assertThat(naverBookApiClient().findByIsbn("9788936433598")).isEmpty();
    }
}
