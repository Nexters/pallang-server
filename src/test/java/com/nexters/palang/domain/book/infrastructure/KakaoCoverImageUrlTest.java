package com.nexters.palang.domain.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KakaoCoverImageUrlTest {

    private static final String THUMBNAIL = "https://search1.kakaocdn.net/thumb/R120x174.q85/"
            + "?fname=http%3A%2F%2Ft1.daumcdn.net%2Flbook%2Fimage%2F7271144%3Ftimestamp%3D20261003131118";

    @Test
    @DisplayName("카카오 썸네일 URL이면 fname의 원본 URL을 https로 바꿔 돌려준다")
    void convertsThumbnailToHttpsOriginal() {
        assertThat(KakaoCoverImageUrl.toOriginal(THUMBNAIL))
                .isEqualTo("https://t1.daumcdn.net/lbook/image/7271144?timestamp=20261003131118");
    }

    @Test
    @DisplayName("fname이 이미 https면 그대로 쓴다")
    void keepsHttpsOriginal() {
        String thumbnail = "https://search1.kakaocdn.net/thumb/R120x174.q85/"
                + "?fname=https%3A%2F%2Ft1.daumcdn.net%2Flbook%2Fimage%2F1";

        assertThat(KakaoCoverImageUrl.toOriginal(thumbnail)).isEqualTo("https://t1.daumcdn.net/lbook/image/1");
    }

    @Test
    @DisplayName("카카오 썸네일이 아닌 URL과 null은 그대로 돌려준다")
    void returnsNonThumbnailUrlAsIs() {
        assertThat(KakaoCoverImageUrl.toOriginal("https://image.aladin.co.kr/product/1/1/cover200/1.jpg"))
                .isEqualTo("https://image.aladin.co.kr/product/1/1/cover200/1.jpg");
        assertThat(KakaoCoverImageUrl.toOriginal("https://api.pallang.co.kr/images/book-covers/1.jpg"))
                .isEqualTo("https://api.pallang.co.kr/images/book-covers/1.jpg");
        assertThat(KakaoCoverImageUrl.toOriginal(null)).isNull();
    }

    @Test
    @DisplayName("fname이 없거나 Daum CDN이 아닌 원본은 썸네일을 그대로 둔다")
    void keepsThumbnailWhenOriginalIsMissingOrUntrusted() {
        String noFname = "https://search1.kakaocdn.net/thumb/R120x174.q85/?other=1";
        String otherHost = "https://search1.kakaocdn.net/thumb/R120x174.q85/?fname=http%3A%2F%2Fevil.com%2Fa.jpg";
        String lookalikeHost = "https://search1.kakaocdn.net/thumb/R120x174.q85/"
                + "?fname=http%3A%2F%2Fdaumcdn.net.evil.com%2Fa.jpg";

        assertThat(KakaoCoverImageUrl.toOriginal(noFname)).isEqualTo(noFname);
        assertThat(KakaoCoverImageUrl.toOriginal(otherHost)).isEqualTo(otherHost);
        assertThat(KakaoCoverImageUrl.toOriginal(lookalikeHost)).isEqualTo(lookalikeHost);
    }
}
