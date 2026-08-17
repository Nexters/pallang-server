package com.nexters.palang.domain.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.nexters.palang.domain.book.application.ExternalBookResult;
import com.nexters.palang.domain.book.common.error.BookException;
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

        AladinSearchResult result = aladinBookApiClient().search("프랑켄슈타인", 0, 20);

        assertThat(result.items()).containsExactly(
                new ExternalBookResult("프랑켄슈타인", "메리 셸리", "문학동네", "9788954429721",
                        "https://image.aladin.co.kr/cover.jpg"));
        assertThat(result.totalResults()).isEqualTo(1);
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 결과를 반환한다")
    void searchReturnsEmptyResultWhenNoItems() {
        stubSearch("{}");

        AladinSearchResult result = aladinBookApiClient().search("없는 책", 0, 20);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalResults()).isZero();
    }

    @Test
    @DisplayName("알라딘 검색 요청이 실패하면 BookException을 던진다")
    void searchThrowsBookExceptionWhenAladinFails() {
        given(exchangeFunction.exchange(any())).willReturn(
                Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).body("error").build()));

        assertThatThrownBy(() -> aladinBookApiClient().search("프랑켄슈타인", 0, 20))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("offset/maxResults를 알라딘의 1부터 시작하는 start/MaxResults 파라미터로 변환해서 요청한다")
    void searchConvertsOffsetToAladinStartAndMaxResults() {
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        given(exchangeFunction.exchange(requestCaptor.capture())).willReturn(Mono.just(jsonResponse("{}")));

        aladinBookApiClient().search("프랑켄슈타인", 5, 5);

        String query = requestCaptor.getValue().url().getQuery();
        assertThat(query).contains("start=6").contains("MaxResults=5");
    }

    @Test
    @DisplayName("고화질 커버 이미지를 받기 위해 Cover=Big 파라미터를 함께 요청한다")
    void searchRequestsBigCoverImage() {
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        given(exchangeFunction.exchange(requestCaptor.capture())).willReturn(Mono.just(jsonResponse("{}")));

        aladinBookApiClient().search("프랑켄슈타인", 0, 20);

        String query = requestCaptor.getValue().url().getQuery();
        assertThat(query).contains("Cover=Big");
    }
}
