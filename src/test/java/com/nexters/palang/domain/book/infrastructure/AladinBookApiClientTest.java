package com.nexters.palang.domain.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.nexters.palang.domain.book.application.ExternalBookResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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

    @Test
    @DisplayName("알라딘 응답을 클라이언트 독립적인 ExternalBookResult로 변환한다")
    void searchMapsAladinResponseToExternalBookResult() {
        String json = """
                {
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
                """;
        given(exchangeFunction.exchange(any())).willReturn(Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(json)
                        .build()));

        List<ExternalBookResult> results = aladinBookApiClient().search("프랑켄슈타인");

        assertThat(results).containsExactly(
                new ExternalBookResult("프랑켄슈타인", "메리 셸리", "문학동네", "9788954429721",
                        "https://image.aladin.co.kr/cover.jpg"));
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 목록을 반환한다")
    void searchReturnsEmptyListWhenNoItems() {
        given(exchangeFunction.exchange(any())).willReturn(Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body("{}")
                        .build()));

        List<ExternalBookResult> results = aladinBookApiClient().search("없는 책");

        assertThat(results).isEmpty();
    }
}
