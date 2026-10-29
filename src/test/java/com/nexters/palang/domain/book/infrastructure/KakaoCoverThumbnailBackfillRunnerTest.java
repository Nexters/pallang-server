package com.nexters.palang.domain.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class KakaoCoverThumbnailBackfillRunnerTest {

    private static final String THUMBNAIL = "https://search1.kakaocdn.net/thumb/R120x174.q85/"
            + "?fname=http%3A%2F%2Ft1.daumcdn.net%2Flbook%2Fimage%2F7271144%3Ftimestamp%3D20261003131118";
    private static final String ORIGINAL = "https://t1.daumcdn.net/lbook/image/7271144?timestamp=20261003131118";
    private static final String THUMBNAIL_WITHOUT_FNAME = "https://search1.kakaocdn.net/thumb/R120x174.q85/?x=1";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private KakaoCoverThumbnailBackfillRunner runner(int batchSize) {
        return new KakaoCoverThumbnailBackfillRunner(bookRepository, transactionManager, batchSize);
    }

    private Book saveBook(String coverImageUrl) {
        return entityManager.persistAndFlush(Book.builder()
                .title("책")
                .author("작가")
                .publisher("출판사")
                .pageCount(100)
                .isbn("9788936433598")
                .coverImageUrl(coverImageUrl)
                .build());
    }

    @Test
    @DisplayName("카카오 썸네일 표지는 원본 URL로 교체하고, 다른 표지는 건드리지 않는다")
    void replacesThumbnailCoverWithOriginal() {
        Book thumbnail = saveBook(THUMBNAIL);
        Book uploaded = saveBook("https://api.pallang.co.kr/images/book-covers/1.jpg");
        Book noCover = saveBook(null);
        entityManager.clear();

        runner(500).run(null);

        assertThat(entityManager.find(Book.class, thumbnail.getId()).getCoverImageUrl()).isEqualTo(ORIGINAL);
        assertThat(entityManager.find(Book.class, uploaded.getId()).getCoverImageUrl())
                .isEqualTo("https://api.pallang.co.kr/images/book-covers/1.jpg");
        assertThat(entityManager.find(Book.class, noCover.getId()).getCoverImageUrl()).isNull();
    }

    @Test
    @DisplayName("원본을 꺼낼 수 없는 썸네일은 그대로 두고, 여러 배치를 돌아도 무한 루프 없이 나머지를 처리한다")
    void keepsThumbnailWithoutOriginalAndContinues() {
        Book withoutFname = saveBook(THUMBNAIL_WITHOUT_FNAME);
        Book first = saveBook(THUMBNAIL);
        Book second = saveBook(THUMBNAIL);
        entityManager.clear();

        runner(1).run(null);

        assertThat(entityManager.find(Book.class, withoutFname.getId()).getCoverImageUrl())
                .isEqualTo(THUMBNAIL_WITHOUT_FNAME);
        assertThat(entityManager.find(Book.class, first.getId()).getCoverImageUrl()).isEqualTo(ORIGINAL);
        assertThat(entityManager.find(Book.class, second.getId()).getCoverImageUrl()).isEqualTo(ORIGINAL);
    }
}
