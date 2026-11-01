package com.nexters.palang.domain.book.infrastructure;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

// 카카오 책 검색의 thumbnail은 search1.kakaocdn.net/thumb/R120x174.q85/?fname=<원본 URL> 형태의
// 썸네일 프록시 URL이라 120x174로 줄어든 이미지가 내려온다(알라딘 cover200보다도 작다). fname에 담긴
// 원본(t1.daumcdn.net/lbook/image/...)은 458x672 정도라, 표지는 썸네일 대신 원본 URL을 쓴다.
// 원본은 fname에 http://로 들어 있지만 https로도 그대로 내려오므로 https로 바꾼다(혼합 콘텐츠 방지).
// 썸네일 크기(R120x174)를 바꾸는 방법은 문서화돼 있지 않고 허용되지 않은 크기는 403을 주므로 쓰지 않는다.
public final class KakaoCoverImageUrl {

    static final String THUMBNAIL_HOST = "search1.kakaocdn.net";
    static final String THUMBNAIL_URL_KEYWORD = THUMBNAIL_HOST + "/thumb/";
    private static final String ORIGINAL_HOST_SUFFIX = ".daumcdn.net";

    private KakaoCoverImageUrl() {
    }

    // 카카오 썸네일 URL이면 원본 URL로 바꾸고, 그 밖의 URL이나 원본을 꺼낼 수 없는 URL은 그대로 돌려준다.
    public static String toOriginal(String url) {
        if (url == null || !url.contains(THUMBNAIL_URL_KEYWORD)) {
            return url;
        }
        return originalOf(url).orElse(url);
    }

    private static Optional<String> originalOf(String thumbnailUrl) {
        try {
            URI thumbnail = URI.create(thumbnailUrl);
            if (!THUMBNAIL_HOST.equals(thumbnail.getHost()) || thumbnail.getRawQuery() == null) {
                return Optional.empty();
            }
            return Arrays.stream(thumbnail.getRawQuery().split("&"))
                    .filter(param -> param.startsWith("fname="))
                    .map(param -> URLDecoder.decode(param.substring("fname=".length()), StandardCharsets.UTF_8))
                    .findFirst()
                    .flatMap(KakaoCoverImageUrl::toHttpsDaumCdnUrl);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // SSRF·엉뚱한 호스트 방지를 위해 원본은 Daum CDN 이미지일 때만 받아들인다.
    private static Optional<String> toHttpsDaumCdnUrl(String original) {
        URI uri = URI.create(original);
        String host = uri.getHost();
        if (host == null || !host.endsWith(ORIGINAL_HOST_SUFFIX)
                || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            return Optional.empty();
        }
        return Optional.of("https" + original.substring(uri.getScheme().length()));
    }
}
