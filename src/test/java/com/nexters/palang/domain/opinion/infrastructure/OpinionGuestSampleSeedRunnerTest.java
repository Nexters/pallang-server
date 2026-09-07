package com.nexters.palang.domain.opinion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.SampleLibraryBook;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import com.nexters.palang.domain.user.domain.GuestSampleAccount;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import com.nexters.palang.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class OpinionGuestSampleSeedRunnerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private OpinionRepository opinionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private OpinionGuestSampleSeedRunner runner() {
        return new OpinionGuestSampleSeedRunner(
                userRepository, bookRepository, passageRepository, opinionRepository, transactionManager);
    }

    @Test
    @DisplayName("ISBN으로 샘플 도서를 찾을 수 있으면 샘플 계정과 대목/의견을 만든다")
    void seedsSampleOpinionsWhenSampleBookExists() {
        bookRepository.save(Book.builder()
                .title(SampleLibraryBook.TITLE)
                .author(SampleLibraryBook.AUTHOR)
                .publisher(SampleLibraryBook.PUBLISHER)
                .pageCount(SampleLibraryBook.PAGE_COUNT)
                .isbn(SampleLibraryBook.ISBN)
                .build());

        runner().run(null);

        assertThat(passageRepository.count()).isEqualTo(2);
        assertThat(opinionRepository.count()).isEqualTo(2);
        assertThat(userRepository.findBySnsProviderAndSnsId(GuestSampleAccount.SNS_PROVIDER, GuestSampleAccount.SNS_ID))
                .isPresent();
    }

    @Test
    @DisplayName("샘플 도서(ISBN)가 아직 없는 환경에서는 아무것도 만들지 않는다")
    void skipsWhenSampleBookMissing() {
        runner().run(null);

        assertThat(passageRepository.count()).isZero();
        assertThat(opinionRepository.count()).isZero();
    }

    @Test
    @DisplayName("이미 씨딩되어 있으면 다시 실행해도 중복 생성하지 않는다")
    void doesNotDuplicateWhenAlreadySeeded() {
        bookRepository.save(Book.builder()
                .title(SampleLibraryBook.TITLE)
                .author(SampleLibraryBook.AUTHOR)
                .publisher(SampleLibraryBook.PUBLISHER)
                .pageCount(SampleLibraryBook.PAGE_COUNT)
                .isbn(SampleLibraryBook.ISBN)
                .build());
        OpinionGuestSampleSeedRunner runner = runner();
        runner.run(null);

        runner.run(null);

        assertThat(passageRepository.count()).isEqualTo(2);
        assertThat(opinionRepository.count()).isEqualTo(2);
    }
}
