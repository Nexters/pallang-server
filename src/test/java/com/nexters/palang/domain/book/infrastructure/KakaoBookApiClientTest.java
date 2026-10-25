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
class KakaoBookApiClientTest {

    @Mock
    private ExchangeFunction exchangeFunction;

    private KakaoBookApiClient kakaoBookApiClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .exchangeFunction(exchangeFunction)
                .build();
        return new KakaoBookApiClient(webClient, "test-rest-api-key");
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

    // count개의 더미 도서 항목으로 이뤄진 카카오 응답 JSON을 만든다. isbn은 idPrefix로 서로 구분되게 채운다.
    private static String documentsJson(int count, long pageableCount, boolean isEnd, String idPrefix) {
        String documents = IntStream.rangeClosed(1, count)
                .mapToObj(i -> """
                        { "title": "책%d", "authors": ["작가"], "publisher": "출판사", "isbn": "%s%d", "thumbnail": "cover%d.jpg" }
                        """.formatted(i, idPrefix, i, i))
                .collect(Collectors.joining(","));
        return """
                { "meta": { "total_count": %d, "pageable_count": %d, "is_end": %s }, "documents": [%s] }
                """.formatted(pageableCount, pageableCount, isEnd, documents);
    }

    @Test
    @DisplayName("검색 결과를 ExternalBookResult로 변환하며, ISBN은 13자리 값을 고르고 저자 배열은 쉼표로 잇는다")
    void searchMapsDocuments() {
        stubSearch("""
                {
                  "meta": { "total_count": 1, "pageable_count": 1, "is_end": true },
                  "documents": [
                    {
                      "title": "프랑켄슈타인",
                      "authors": ["메리 셸리", "김선형"],
                      "publisher": "문학동네",
                      "isbn": "8954429726 9788954429721",
                      "thumbnail": "https://search1.kakaocdn.net/thumb/cover.jpg"
                    }
                  ]
                }
                """);

        ExternalBookSearchResult result = kakaoBookApiClient().searchAll("프랑켄슈타인");

        assertThat(result.items()).containsExactly(
                new ExternalBookResult("프랑켄슈타인", "메리 셸리, 김선형", "문학동네", "9788954429721",
                        "https://search1.kakaocdn.net/thumb/cover.jpg"));
        assertThat(result.totalResults()).isEqualTo(1);
    }

    @Test
    @DisplayName("썸네일이 빈 문자열이면 표지 URL을 null로 둔다")
    void searchTreatsBlankThumbnailAsNull() {
        stubSearch("""
                {
                  "meta": { "total_count": 1, "pageable_count": 1, "is_end": true },
                  "documents": [
                    { "title": "죄와 벌", "authors": [], "publisher": "민음사", "isbn": "9788937461033", "thumbnail": "" }
                  ]
                }
                """);

        ExternalBookSearchResult result = kakaoBookApiClient().searchAll("죄와 벌");

        assertThat(result.items()).containsExactly(
                new ExternalBookResult("죄와 벌", "", "민음사", "9788937461033", null));
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 결과를 반환한다")
    void searchReturnsEmptyResultWhenNoDocuments() {
        stubSearch("{ \"meta\": { \"total_count\": 0, \"pageable_count\": 0, \"is_end\": true }, \"documents\": [] }");

        ExternalBookSearchResult result = kakaoBookApiClient().searchAll("없는 책");

        assertThat(result.items()).isEmpty();
        assertThat(result.totalResults()).isZero();
    }

    @Test
    @DisplayName("카카오 검색 요청이 실패하면 BookException을 던진다")
    void searchThrowsBookExceptionWhenKakaoFails() {
        given(exchangeFunction.exchange(any())).willReturn(
                Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).body("error").build()));

        assertThatThrownBy(() -> kakaoBookApiClient().searchAll("프랑켄슈타인"))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("REST API 키를 KakaoAK 헤더로 보내고, 제목 대상(target=title)으로 page=1, size=50부터 요청한다")
    void searchSendsAuthorizationHeaderAndFirstPageParams() {
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        given(exchangeFunction.exchange(requestCaptor.capture())).willReturn(Mono.just(jsonResponse("{}")));

        kakaoBookApiClient().searchAll("프랑켄슈타인");

        ClientRequest request = requestCaptor.getValue();
        assertThat(request.url().getPath()).isEqualTo("/v3/search/book");
        assertThat(request.url().getQuery()).contains("target=title").contains("page=1").contains("size=50");
        assertThat(request.headers().getFirst("Authorization")).isEqualTo("KakaoAK test-rest-api-key");
    }

    @Test
    @DisplayName("is_end가 false면 다음 page를 이어서 요청해 합치고, true가 되면 멈춘다")
    void searchAllFetchesNextPageUntilIsEnd() {
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        given(exchangeFunction.exchange(requestCaptor.capture())).willReturn(
                Mono.just(jsonResponse(documentsJson(50, 70, false, "first-"))),
                Mono.just(jsonResponse(documentsJson(20, 70, true, "second-"))));

        ExternalBookSearchResult result = kakaoBookApiClient().searchAll("책모음");

        assertThat(result.items()).hasSize(70);
        assertThat(result.totalResults()).isEqualTo(70);
        List<String> queries = requestCaptor.getAllValues().stream()
                .map(request -> request.url().getQuery())
                .toList();
        assertThat(queries.get(0)).contains("page=1");
        assertThat(queries.get(1)).contains("page=2");
        verify(exchangeFunction, times(2)).exchange(any());
    }

    @Test
    @DisplayName("결과가 200건을 넘어가면 더 요청하지 않고 200건(4페이지)까지만 모은다")
    void searchAllStopsAtMaxTotalResultsCap() {
        given(exchangeFunction.exchange(any())).willReturn(
                Mono.just(jsonResponse(documentsJson(50, 9999, false, "a-"))),
                Mono.just(jsonResponse(documentsJson(50, 9999, false, "b-"))),
                Mono.just(jsonResponse(documentsJson(50, 9999, false, "c-"))),
                Mono.just(jsonResponse(documentsJson(50, 9999, false, "d-"))));

        ExternalBookSearchResult result = kakaoBookApiClient().searchAll("인기키워드");

        assertThat(result.items()).hasSize(200);
        verify(exchangeFunction, times(4)).exchange(any());
    }

    @Test
    @DisplayName("ISBN 대상(target=isbn)으로 검색해 ISBN이 정확히 일치하는 도서만 반환한다")
    void findByIsbnReturnsOnlyExactIsbnMatch() {
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        given(exchangeFunction.exchange(requestCaptor.capture())).willReturn(Mono.just(jsonResponse("""
                {
                  "meta": { "total_count": 2, "pageable_count": 2, "is_end": true },
                  "documents": [
                    { "title": "다른 책", "isbn": "9788936433000", "thumbnail": "other.jpg" },
                    { "title": "채식주의자", "isbn": "8936433598 9788936433598", "thumbnail": "kakao.jpg" }
                  ]
                }
                """)));

        assertThat(kakaoBookApiClient().findByIsbn("9788936433598"))
                .map(ExternalBookResult::coverImageUrl)
                .contains("kakao.jpg");
        assertThat(requestCaptor.getValue().url().getQuery()).contains("target=isbn");
    }

    @Test
    @DisplayName("ISBN이 일치하는 도서가 없으면 빈 Optional을 반환한다")
    void findByIsbnReturnsEmptyWhenNoExactMatch() {
        stubSearch("""
                { "documents": [ { "title": "다른 책", "isbn": "9788936433000", "thumbnail": "other.jpg" } ] }
                """);

        assertThat(kakaoBookApiClient().findByIsbn("9788936433598")).isEmpty();
    }
}
