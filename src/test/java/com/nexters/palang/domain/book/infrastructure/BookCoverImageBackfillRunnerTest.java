package com.nexters.palang.domain.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.application.ExternalBookResult;
import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class BookCoverImageBackfillRunnerTest {

    private static final String ALADIN_COVER = "https://image.aladin.co.kr/product/1/1/cover/1.jpg";
    private static final String KAKAO_COVER = "https://search1.kakaocdn.net/thumb/R120x174.q85/?fname=1.jpg";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private KakaoBookApiClient kakaoBookApiClient;

    @BeforeEach
    void setUp() {
        kakaoBookApiClient = mock(KakaoBookApiClient.class);
    }

    private BookCoverImageBackfillRunner runner(int batchSize) {
        return new BookCoverImageBackfillRunner(bookRepository, kakaoBookApiClient, transactionManager, batchSize);
    }

    private Book saveBook(String isbn, String coverImageUrl) {
        return entityManager.persistAndFlush(Book.builder()
                .title("책")
                .author("작가")
                .publisher("출판사")
                .pageCount(100)
                .isbn(isbn)
                .coverImageUrl(coverImageUrl)
                .build());
    }

    private static ExternalBookResult kakaoResult(String isbn, String cover) {
        return new ExternalBookResult("책", "작가", "출판사", isbn, cover);
    }

    @Test
    @DisplayName("알라딘 표지를 가진 도서는 ISBN으로 카카오에서 찾은 표지로 교체한다")
    void replacesAladinCoverWithKakaoCover() {
        Book book = saveBook("9788936433598", ALADIN_COVER);
        entityManager.clear();
        given(kakaoBookApiClient.findByIsbn("9788936433598"))
                .willReturn(Optional.of(kakaoResult("9788936433598", KAKAO_COVER)));

        runner(100).run();

        assertThat(entityManager.find(Book.class, book.getId()).getCoverImageUrl()).isEqualTo(KAKAO_COVER);
    }

    @Test
    @DisplayName("카카오에서 찾지 못했거나 조회가 실패한 도서는 기존 표지를 유지하고 나머지는 계속 처리한다")
    void keepsAladinCoverWhenKakaoLookupFails() {
        Book notFound = saveBook("isbn-not-found", ALADIN_COVER);
        Book failed = saveBook("isbn-failed", ALADIN_COVER);
        Book found = saveBook("isbn-found", ALADIN_COVER);
        entityManager.clear();
        given(kakaoBookApiClient.findByIsbn("isbn-not-found")).willReturn(Optional.empty());
        given(kakaoBookApiClient.findByIsbn("isbn-failed"))
                .willThrow(new BookException(BookErrorCode.EXTERNAL_SEARCH_FAILED));
        given(kakaoBookApiClient.findByIsbn("isbn-found"))
                .willReturn(Optional.of(kakaoResult("isbn-found", KAKAO_COVER)));

        // 배치 크기 1로 여러 배치를 돌면서, 교체되지 않고 남은 도서 때문에 무한 루프를 돌지 않는지도 검증한다.
        runner(1).run();

        assertThat(entityManager.find(Book.class, notFound.getId()).getCoverImageUrl()).isEqualTo(ALADIN_COVER);
        assertThat(entityManager.find(Book.class, failed.getId()).getCoverImageUrl()).isEqualTo(ALADIN_COVER);
        assertThat(entityManager.find(Book.class, found.getId()).getCoverImageUrl()).isEqualTo(KAKAO_COVER);
    }

    @Test
    @DisplayName("알라딘 표지가 아닌 도서와 ISBN이 없는 도서는 카카오를 호출하지 않는다")
    void skipsNonAladinCoversAndBooksWithoutIsbn() {
        saveBook("9788936433598", "/images/book-covers/uploaded.jpg");
        Book noIsbn = saveBook(null, ALADIN_COVER);
        entityManager.clear();

        runner(100).run();

        assertThat(entityManager.find(Book.class, noIsbn.getId()).getCoverImageUrl()).isEqualTo(ALADIN_COVER);
        verify(kakaoBookApiClient, never()).findByIsbn(anyString());
    }

    @Test
    @DisplayName("카카오 REST API 키가 설정되지 않은 환경에서는 백필을 실행하지 않는다")
    void doesNothingWhenKakaoIsNotConfigured() {
        saveBook("9788936433598", ALADIN_COVER);
        given(kakaoBookApiClient.isConfigured()).willReturn(false);

        runner(100).onApplicationReady();

        verify(kakaoBookApiClient, never()).findByIsbn(anyString());
    }
}
