package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.application.ExternalBookResult;
import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.infrastructure.dto.KakaoBookDocument;
import com.nexters.palang.domain.book.infrastructure.dto.KakaoBookSearchResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

// 알라딘 TTB API 서비스 종료(2026-10-30)로 카카오(Daum) 책 검색 API로 교체했다.
// (네이버 책 검색 API도 2026-07-31에 종료돼 후보에서 제외.)
// https://developers.kakao.com/docs/latest/ko/daum-search/dev-guide#search-book
@Component
public class KakaoBookApiClient {

    private static final String SEARCH_PATH = "/v3/search/book";
    // 카카오 책 검색의 한도: size는 한 번 요청에 최대 50건, page는 최대 50이다. 키워드당 결과는
    // 알라딘을 쓰던 때와 같이 200건(50건 x 4페이지)까지만 모은다.
    private static final int MAX_RESULTS_PER_CALL = 50;
    private static final int MAX_TOTAL_RESULTS = 200;
    private static final int ISBN_LOOKUP_SIZE = 10;

    private final WebClient kakaoBookWebClient;
    private final String restApiKey;

    public KakaoBookApiClient(WebClient kakaoBookWebClient, @Value("${kakao-book.rest-api-key}") String restApiKey) {
        this.kakaoBookWebClient = kakaoBookWebClient;
        this.restApiKey = restApiKey;
    }

    public boolean isConfigured() {
        return !restApiKey.isBlank();
    }

    // GET /api/books/search는 DB 검색 결과와 하나로 이어붙인 목록처럼 페이지네이션하면서 page 경계에
    // 맞지 않는 임의 offset이 필요하므로, 여기서는 키워드당 결과 전체(최대 200건)를 미리 다 모아서
    // 반환하고, 실제 offset/limit 슬라이싱은 호출부(BookService)가 이 리스트를 대상으로 메모리에서 처리한다.
    //
    // 자동완성이 타이핑마다 이 메서드를 호출하므로 @Cacheable로 캐싱한다 (키: keyword, TTL은
    // CacheConfig 참고). 실패(예외)는 Spring 캐시 추상화가 애초에 캐싱하지 않고, 빈 결과는 unless로
    // 캐싱에서 제외해 다음 호출에서 다시 시도하게 한다.
    // 이 메서드는 반드시 다른 빈(BookService)이 호출해야 한다 — 같은 클래스 안에서 self-invocation하면
    // Spring AOP 프록시를 거치지 않아 캐싱이 동작하지 않는다.
    @Cacheable(value = "kakaoBookSearch", key = "#keyword", unless = "#result.items().isEmpty()")
    public ExternalBookSearchResult searchAll(String keyword) {
        List<ExternalBookResult> items = new ArrayList<>();
        long totalResults = 0;

        for (int page = 1; items.size() < MAX_TOTAL_RESULTS; page++) {
            KakaoBookSearchResponse response = fetch(keyword, "title", page, MAX_RESULTS_PER_CALL);
            if (response == null || response.documents() == null || response.documents().isEmpty()) {
                break;
            }

            totalResults = totalResults(response, items.size() + response.documents().size());
            items.addAll(response.documents().stream().map(this::toExternalBookResult).toList());

            if (isEnd(response)) {
                break;
            }
        }

        if (items.isEmpty()) {
            return ExternalBookSearchResult.empty();
        }
        return new ExternalBookSearchResult(items, totalResults);
    }

    // ISBN 검색(target=isbn) 결과 중 ISBN이 정확히 일치하는 항목만 고른다.
    // 캐싱하지 않는다(BookCoverImageBackfillRunner에서만 사용).
    public Optional<ExternalBookResult> findByIsbn(String isbn) {
        KakaoBookSearchResponse response = fetch(isbn, "isbn", 1, ISBN_LOOKUP_SIZE);
        if (response == null || response.documents() == null) {
            return Optional.empty();
        }
        return response.documents().stream()
                .map(this::toExternalBookResult)
                .filter(result -> isbn.equals(result.isbn()))
                .findFirst();
    }

    private KakaoBookSearchResponse fetch(String query, String target, int page, int size) {
        try {
            return kakaoBookWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(SEARCH_PATH)
                            .queryParam("query", query)
                            .queryParam("target", target)
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .queryParam("sort", "accuracy")
                            .build())
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .retrieve()
                    .bodyToMono(KakaoBookSearchResponse.class)
                    .block();
        } catch (RuntimeException e) {
            throw new BookException(BookErrorCode.EXTERNAL_SEARCH_FAILED, e);
        }
    }

    // total_count는 중복 도서까지 센 값이라, 실제로 페이지를 넘겨 받을 수 있는 pageable_count를 우선 쓴다.
    private static long totalResults(KakaoBookSearchResponse response, long fallback) {
        KakaoBookSearchResponse.Meta meta = response.meta();
        if (meta == null) {
            return fallback;
        }
        if (meta.pageableCount() != null) {
            return meta.pageableCount();
        }
        return meta.totalCount() != null ? meta.totalCount() : fallback;
    }

    private static boolean isEnd(KakaoBookSearchResponse response) {
        if (response.meta() != null && response.meta().isEnd() != null) {
            return response.meta().isEnd();
        }
        return response.documents().size() < MAX_RESULTS_PER_CALL;
    }

    private ExternalBookResult toExternalBookResult(KakaoBookDocument document) {
        String authors = document.authors() == null ? null : String.join(", ", document.authors());
        return new ExternalBookResult(
                document.title(),
                authors,
                document.publisher(),
                isbn13(document.isbn()),
                blankToNull(document.thumbnail()));
    }

    // "ISBN10 ISBN13" 중 13자리를 우선 고르고, 13자리가 없으면 마지막 값을 쓴다.
    private static String isbn13(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return null;
        }
        String[] tokens = isbn.trim().split("\\s+");
        return Arrays.stream(tokens)
                .filter(token -> token.length() == 13)
                .findFirst()
                .orElse(tokens[tokens.length - 1]);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
