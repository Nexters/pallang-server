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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    @DisplayName("검색 결과를 반환하며, 응답 속도를 위해 pageCount는 채우지 않고 항상 null이다")
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

        Page<ExternalBookResult> results = aladinBookApiClient().search("프랑켄슈타인", PageRequest.of(0, 20));

        assertThat(results.getContent()).containsExactly(
                new ExternalBookResult("프랑켄슈타인", "메리 셸리", "문학동네", null, "9788954429721",
                        "https://image.aladin.co.kr/cover.jpg"));
        assertThat(results.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 페이지를 반환한다")
    void searchReturnsEmptyPageWhenNoItems() {
        stubSearch("{}");

        Page<ExternalBookResult> results = aladinBookApiClient().search("없는 책", PageRequest.of(0, 20));

        assertThat(results.getContent()).isEmpty();
        assertThat(results.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("알라딘 검색 요청이 실패하면 BookException을 던진다")
    void searchThrowsBookExceptionWhenAladinFails() {
        given(exchangeFunction.exchange(any())).willReturn(
                Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).body("error").build()));

        assertThatThrownBy(() -> aladinBookApiClient().search("프랑켄슈타인", PageRequest.of(0, 20)))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("page/size를 알라딘의 1부터 시작하는 start/MaxResults 파라미터로 변환해서 요청한다")
    void searchConvertsPageableToAladinStartAndMaxResults() {
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        given(exchangeFunction.exchange(requestCaptor.capture())).willReturn(Mono.just(jsonResponse("{}")));

        Pageable secondPageOfFive = PageRequest.of(1, 5);
        aladinBookApiClient().search("프랑켄슈타인", secondPageOfFive);

        String query = requestCaptor.getValue().url().getQuery();
        assertThat(query).contains("start=6").contains("MaxResults=5");
    }
}
