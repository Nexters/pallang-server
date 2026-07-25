package com.nexters.palang.domain.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.application.BookActivityProjection;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class BookQueryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    private BookQueryRepository bookQueryRepository;

    @BeforeEach
    void setUp() {
        bookQueryRepository = new BookQueryRepository(new JPAQueryFactory(entityManager.getEntityManager()));
    }

    private User user(String snsId) {
        return entityManager.persistAndFlush(User.builder()
                .nickname("닉네임" + snsId)
                .snsProvider(SnsProvider.KAKAO)
                .snsId(snsId)
                .termsAgreedAt(LocalDateTime.now())
                .build());
    }

    private Book book(String title) {
        return entityManager.persistAndFlush(Book.builder()
                .title(title)
                .author("작가")
                .publisher("출판사")
                .pageCount(300)
                .build());
    }

    private Passage passage(Book book, User creator, int pageNumber) {
        return entityManager.persistAndFlush(Passage.builder()
                .book(book)
                .creator(creator)
                .pageNumber(pageNumber)
                .quotedText("발췌 문장")
                .isSpoiler(false)
                .normalizedHash("hash-" + book.getId() + "-" + pageNumber)
                .build());
    }

    private Opinion opinion(Passage passage, User user) {
        return entityManager.persistAndFlush(Opinion.builder()
                .passage(passage)
                .user(user)
                .content("흔적 내용")
                .build());
    }

    @Test
    @DisplayName("대목이 있는 도서만 캐러셀에 노출되고 대목/흔적 수가 함께 집계된다")
    void findCarouselBooksOnlyIncludesBooksWithPassages() {
        User writer = user("writer-1");
        Book bookWithPassages = book("흔적이 있는 책");
        Book bookWithoutPassages = book("흔적이 없는 책");
        Passage passage1 = passage(bookWithPassages, writer, 10);
        passage(bookWithPassages, writer, 20);
        opinion(passage1, writer);
        opinion(passage1, writer);

        List<BookActivityProjection> results = bookQueryRepository.findCarouselBooks();

        assertThat(results).extracting(BookActivityProjection::bookId)
                .containsExactly(bookWithPassages.getId());
        assertThat(results.get(0).passageCount()).isEqualTo(2);
        assertThat(results.get(0).opinionCount()).isEqualTo(2);
        assertThat(bookWithoutPassages.getId()).isNotIn(
                results.stream().map(BookActivityProjection::bookId).toList());
    }

    @Test
    @DisplayName("흔적이 많은 순으로 인기 도서를 조회한다")
    void findPopularBooksOrdersByOpinionCountDescending() {
        User writer = user("writer-2");
        Book popularBook = book("인기 도서");
        Book lessPopularBook = book("비인기 도서");
        Passage popularPassage = passage(popularBook, writer, 1);
        Passage lessPopularPassage = passage(lessPopularBook, writer, 1);
        opinion(popularPassage, writer);
        opinion(popularPassage, writer);
        opinion(lessPopularPassage, writer);

        List<BookActivityProjection> results = bookQueryRepository.findPopularBooks(10);

        assertThat(results).extracting(BookActivityProjection::bookId)
                .containsExactly(popularBook.getId(), lessPopularBook.getId());
    }

    @Test
    @DisplayName("내가 최근에 대목을 남긴 도서 id만 조회하고 다른 사용자의 도서는 제외한다")
    void findRecentlyActiveBookIdsOnlyIncludesGivenUser() {
        User me = user("me");
        User other = user("other");
        Book myBook = book("내가 남긴 책");
        Book othersBook = book("다른 사람이 남긴 책");
        passage(myBook, me, 1);
        passage(othersBook, other, 1);

        List<Long> results = bookQueryRepository.findRecentlyActiveBookIds(me.getId(), 10);

        assertThat(results).containsExactly(myBook.getId());
    }
}
