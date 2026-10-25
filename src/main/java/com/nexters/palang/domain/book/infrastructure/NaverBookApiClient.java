package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.application.ExternalBookResult;
import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.infrastructure.dto.NaverBookItem;
import com.nexters.palang.domain.book.infrastructure.dto.NaverBookSearchResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.HtmlUtils;

// 알라딘 TTB API 서비스 종료로 네이버 검색 API(책)로 교체했다.
// https://developers.naver.com/docs/serviceapi/search/book/book.md
@Component
public class NaverBookApiClient {

    private static final String SEARCH_PATH = "/v1/search/book.json";
    // 네이버 책 검색의 한도: display는 한 번 요청에 최대 100건, start(1부터 시작하는 레코드 위치)는
    // 최대 1000이다. 키워드당 결과는 알라딘을 쓰던 때와 같이 200건까지만 모은다.
    private static final int MAX_RESULTS_PER_CALL = 100;
    private static final int MAX_TOTAL_RESULTS = 200;
    private static final int ISBN_LOOKUP_DISPLAY = 10;
    // 검색어와 일치하는 부분을 <b> 태그로 감싸서 내려주는 경우가 있어 제거한다.
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    private final WebClient naverWebClient;
    private final String clientId;
    private final String clientSecret;

    public NaverBookApiClient(
            WebClient naverWebClient,
            @Value("${naver.client-id}") String clientId,
            @Value("${naver.client-secret}") String clientSecret) {
        this.naverWebClient = naverWebClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank();
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
    @Cacheable(value = "naverBookSearch", key = "#keyword", unless = "#result.items().isEmpty()")
    public ExternalBookSearchResult searchAll(String keyword) {
        List<ExternalBookResult> items = new ArrayList<>();
        long totalResults = 0;

        while (items.size() < MAX_TOTAL_RESULTS) {
            int display = Math.min(MAX_RESULTS_PER_CALL, MAX_TOTAL_RESULTS - items.size());
            NaverBookSearchResponse response = fetch(keyword, items.size() + 1, display);
            if (response == null || response.items() == null || response.items().isEmpty()) {
                break;
            }

            totalResults = response.total() != null ? response.total() : items.size() + response.items().size();
            items.addAll(response.items().stream().map(this::toExternalBookResult).toList());

            // 요청한 display보다 적게 왔다면 네이버가 가진 결과를 이미 다 받은 것이다.
            if (response.items().size() < display) {
                break;
            }
        }

        if (items.isEmpty()) {
            return ExternalBookSearchResult.empty();
        }
        return new ExternalBookSearchResult(items, totalResults);
    }

    // 네이버 상세 검색(d_isbn)은 XML 응답만 지원해서, 일반 검색에 ISBN을 검색어로 넣고 결과 중
    // ISBN이 정확히 일치하는 항목만 고른다. 캐싱하지 않는다(BookCoverImageBackfillRunner에서만 사용).
    public Optional<ExternalBookResult> findByIsbn(String isbn) {
        NaverBookSearchResponse response = fetch(isbn, 1, ISBN_LOOKUP_DISPLAY);
        if (response == null || response.items() == null) {
            return Optional.empty();
        }
        return response.items().stream()
                .map(this::toExternalBookResult)
                .filter(result -> isbn.equals(result.isbn()))
                .findFirst();
    }

    private NaverBookSearchResponse fetch(String query, int start, int display) {
        try {
            return naverWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(SEARCH_PATH)
                            .queryParam("query", query)
                            .queryParam("display", display)
                            .queryParam("start", start)
                            .queryParam("sort", "sim")
                            .build())
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .bodyToMono(NaverBookSearchResponse.class)
                    .block();
        } catch (RuntimeException e) {
            throw new BookException(BookErrorCode.EXTERNAL_SEARCH_FAILED, e);
        }
    }

    private ExternalBookResult toExternalBookResult(NaverBookItem item) {
        return new ExternalBookResult(
                clean(item.title()),
                clean(item.author() == null ? null : item.author().replace("^", ", ")),
                clean(item.publisher()),
                isbn13(item.isbn()),
                blankToNull(item.image()));
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        return HtmlUtils.htmlUnescape(HTML_TAG.matcher(value).replaceAll("")).trim();
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
