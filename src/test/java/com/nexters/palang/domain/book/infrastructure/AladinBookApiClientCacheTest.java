package com.nexters.palang.domain.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.common.error.BookException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

// AladinBookApiClient#searchAll()에 걸린 @Cacheable이 실제로 동작하는지 검증한다. @Cacheable은
// Spring AOP 프록시를 거쳐야 동작해서(클래스 안에서 self-invocation하면 적용되지 않음) Mockito로
// AladinBookApiClient를 직접 new하는 방식(AladinBookApiClientTest)으로는 검증할 수 없고, 스프링
// 컨텍스트 + 실제 Redis(로컬 docker-compose 또는 CI redis 서비스)가 필요하다.
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
class AladinBookApiClientCacheTest {

    private static final String CACHE_NAME = "aladinBookSearch";

    @Autowired
    private AladinBookApiClient aladinBookApiClient;

    @Autowired
    private ExchangeFunction exchangeFunction;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        reset(exchangeFunction);
        cacheManager.getCache(CACHE_NAME).clear();
    }

    // Redis write가 반영되는 짧은 지연을 흡수하기 위한 폴링. 최대 200ms(20ms x 10회)까지만
    // 기다리고, 그래도 안 보이면 진짜 버그로 간주해 실패시킨다.
    private void awaitCacheEntry(String cacheKey) throws InterruptedException {
        for (int attempt = 0; attempt < 10; attempt++) {
            if (cacheManager.getCache(CACHE_NAME).get(cacheKey) != null) {
                return;
            }
            Thread.sleep(20);
        }
        assertThat(cacheManager.getCache(CACHE_NAME).get(cacheKey)).isNotNull();
    }

    private static ClientResponse jsonResponse(String json) {
        return ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(json)
                .build();
    }

    // 실제 알라딘 서버 대신 mock ExchangeFunction으로 응답하는 aladinWebClient로 교체한다.
    // 원래 빈과 이름이 같아야 교체되므로, 이 테스트 컨텍스트에 한해 bean overriding을 허용한다.
    @TestConfiguration
    static class StubWebClientConfig {

        @Bean
        public ExchangeFunction exchangeFunction() {
            return mock(ExchangeFunction.class);
        }

        @Bean
        public WebClient aladinWebClient(ExchangeFunction exchangeFunction) {
            return WebClient.builder()
                    .baseUrl("http://www.aladin.co.kr/ttb/api")
                    .exchangeFunction(exchangeFunction)
                    .build();
        }
    }

    @Test
    @DisplayName("같은 키워드로 다시 검색하면 캐시에서 응답하고 알라딘을 다시 호출하지 않는다")
    void searchUsesCacheOnSecondCall() throws InterruptedException {
        given(exchangeFunction.exchange(any())).willReturn(Mono.just(jsonResponse("""
                {
                  "totalResults": 1,
                  "item": [
                    { "title": "채식주의자", "author": "한강", "publisher": "창비",
                      "isbn13": "9788936433598", "cover": "cover.jpg" }
                  ]
                }
                """)));

        AladinSearchResult first = aladinBookApiClient.searchAll("채식주의자캐시테스트");
        assertThat(first.items()).as("첫 호출 결과가 비어있으면 unless 조건 때문에 애초에 캐싱되지 않는다").isNotEmpty();

        // @Cacheable의 캐시 put은 원래 메서드가 결과를 반환하기 "전에" 같은 스레드에서 동기적으로
        // 일어나지만, 전체 테스트 스위트를 함께 돌려 스프링 컨텍스트가 많이 떠 있는 상황에서는 Redis
        // write가 반영되는 데 짧은 지연이 생기는 걸 관찰했다. 캐시가 실제로 없는 버그와 "아직 반영
        // 안 됐을 뿐"인 경우를 구분하기 위해 짧게 재시도하며 기다린다.
        String cacheKey = "채식주의자캐시테스트";
        awaitCacheEntry(cacheKey);

        AladinSearchResult second = aladinBookApiClient.searchAll("채식주의자캐시테스트");

        assertThat(second).isEqualTo(first);
        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    @DisplayName("검색 실패는 캐싱되지 않아 다음 호출에서 알라딘을 다시 호출한다")
    void searchDoesNotCacheFailure() {
        given(exchangeFunction.exchange(any()))
                .willReturn(Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).body("error").build()))
                .willReturn(Mono.just(jsonResponse("{}")));

        assertThatThrownBy(() -> aladinBookApiClient.searchAll("실패캐시테스트"))
                .isInstanceOf(BookException.class);
        AladinSearchResult second = aladinBookApiClient.searchAll("실패캐시테스트");

        assertThat(second.items()).isEmpty();
        verify(exchangeFunction, times(2)).exchange(any());
    }

    @Test
    @DisplayName("빈 결과는 캐싱되지 않아 다음 호출에서 알라딘을 다시 호출한다")
    void searchDoesNotCacheEmptyResult() {
        given(exchangeFunction.exchange(any())).willReturn(Mono.just(jsonResponse("{}")));

        aladinBookApiClient.searchAll("빈결과캐시테스트");
        aladinBookApiClient.searchAll("빈결과캐시테스트");

        verify(exchangeFunction, times(2)).exchange(any());
    }
}
