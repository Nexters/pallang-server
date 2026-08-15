package com.nexters.palang.domain.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.application.BookActivityProjection;
import com.nexters.palang.domain.book.application.BookSearchProjection;
import com.nexters.palang.domain.book.application.BookSearchSort;
import com.nexters.palang.domain.book.application.OpinionCountScope;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DataJpaTest
@Import(JpaAuditingConfig.class)
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
    @DisplayName("검색어와 제목의 띄어쓰기가 달라도 도서를 찾는다")
    void searchByTitleIgnoresWhitespaceDifferences() {
        User writer = user("wsr1");
        Book book = book("두 번째 산책");
        opinion(passage(book, writer, 1), writer);

        Page<BookSearchProjection> results = bookQueryRepository.searchByTitle(
                "두번째산책", BookSearchSort.RECENT, PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(BookSearchProjection::bookId).containsExactly(book.getId());
    }

    @Test
    @DisplayName("흔적(Opinion)이 없는 도서는 검색 결과에서 제외된다")
    void searchByTitleExcludesBooksWithoutOpinions() {
        User writer = user("wsr2");
        Book bookWithoutPassage = book("대목도 없는 책");
        Book bookWithoutOpinion = book("대목만 있는 책");
        passage(bookWithoutOpinion, writer, 1);

        Page<BookSearchProjection> results = bookQueryRepository.searchByTitle("책", BookSearchSort.RECENT,
                PageRequest.of(0, 20));

        assertThat(results.getContent()).isEmpty();
        assertThat(bookWithoutPassage.getId()).isNotNull();
    }

    @Test
    @DisplayName("검색어가 빈 문자열이면 흔적이 있는 전체 도서 목록을 반환한다")
    void searchByTitleReturnsAllBooksWhenKeywordIsBlank() {
        User writer = user("wsr3");
        Book book1 = book("책1");
        Book book2 = book("책2");
        opinion(passage(book1, writer, 1), writer);
        opinion(passage(book2, writer, 1), writer);

        Page<BookSearchProjection> results = bookQueryRepository.searchByTitle(
                "", BookSearchSort.RECENT, PageRequest.of(0, 20));

        assertThat(results.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("이름순 정렬은 제목 오름차순으로 도서를 반환한다")
    void searchByTitleSortsByNameAscending() {
        User writer = user("wsr4");
        Book bookB = book("나책");
        Book bookA = book("가책");
        opinion(passage(bookB, writer, 1), writer);
        opinion(passage(bookA, writer, 1), writer);

        Page<BookSearchProjection> results = bookQueryRepository.searchByTitle(
                "책", BookSearchSort.NAME, PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(BookSearchProjection::bookId)
                .containsExactly(bookA.getId(), bookB.getId());
    }

    @Test
    @DisplayName("최신순 정렬은 가장 최근에 흔적이 남은 도서부터 반환한다")
    void searchByTitleSortsByRecentOpinion() throws InterruptedException {
        User writer = user("wsr5");
        Book olderBook = book("먼저 남긴 책");
        Book newerBook = book("나중에 남긴 책");
        opinion(passage(olderBook, writer, 1), writer);
        Thread.sleep(10);
        opinion(passage(newerBook, writer, 1), writer);

        Page<BookSearchProjection> results = bookQueryRepository.searchByTitle(
                "책", BookSearchSort.RECENT, PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(BookSearchProjection::bookId)
                .containsExactly(newerBook.getId(), olderBook.getId());
    }

    @Test
    @DisplayName("의견순 정렬은 흔적이 많은 도서부터 반환한다")
    void searchByTitleSortsByOpinionCountDescending() {
        User writer = user("wsr6");
        Book popularBook = book("의견 많은 책");
        Book lessPopularBook = book("의견 적은 책");
        Passage popularPassage = passage(popularBook, writer, 1);
        opinion(popularPassage, writer);
        opinion(popularPassage, writer);
        opinion(passage(lessPopularBook, writer, 1), writer);

        Page<BookSearchProjection> results = bookQueryRepository.searchByTitle(
                "책", BookSearchSort.OPINION, PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(BookSearchProjection::bookId)
                .containsExactly(popularBook.getId(), lessPopularBook.getId());
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

        List<BookActivityProjection> results = bookQueryRepository.findCarouselBooks(0, 20);

        assertThat(results).extracting(BookActivityProjection::bookId)
                .containsExactly(bookWithPassages.getId());
        assertThat(results.get(0).passageCount()).isEqualTo(2);
        assertThat(results.get(0).opinionCount()).isEqualTo(2);
        assertThat(bookWithoutPassages.getId()).isNotIn(
                results.stream().map(BookActivityProjection::bookId).toList());
    }

    @Test
    @DisplayName("캐러셀 조회는 요청한 개수만큼만 반환하고 전체 개수를 별도로 알려준다")
    void findCarouselBooksRespectsLimit() {
        User writer = user("writer-page");
        Book book1 = book("책1");
        Book book2 = book("책2");
        passage(book1, writer, 1);
        passage(book2, writer, 1);

        List<BookActivityProjection> firstBook = bookQueryRepository.findCarouselBooks(0, 1);

        assertThat(firstBook).hasSize(1);
        assertThat(bookQueryRepository.countCarouselBooks()).isEqualTo(2);
    }

    @Test
    @DisplayName("임의의 offset을 지정하면 전체 정렬 순서상 그 위치부터 도서를 반환한다")
    void findCarouselBooksSupportsArbitraryOffset() {
        User writer = user("writer-off");
        Book book1 = book("책1");
        Book book2 = book("책2");
        Book book3 = book("책3");
        passage(book1, writer, 1);
        passage(book2, writer, 1);
        passage(book3, writer, 1);

        List<BookActivityProjection> all = bookQueryRepository.findCarouselBooks(0, 3);
        List<BookActivityProjection> middle = bookQueryRepository.findCarouselBooks(1, 1);

        assertThat(middle).extracting(BookActivityProjection::bookId)
                .containsExactly(all.get(1).bookId());
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

        Page<BookActivityProjection> results = bookQueryRepository.findPopularBooks(PageRequest.of(0, 10));

        assertThat(results.getContent()).extracting(BookActivityProjection::bookId)
                .containsExactly(popularBook.getId(), lessPopularBook.getId());
    }

    @Test
    @DisplayName("내가 최근에 흔적을 남긴 도서 id만 조회하고 다른 사용자의 도서는 제외한다")
    void findRecentlyActiveBookIdsOnlyIncludesGivenUser() {
        User me = user("me");
        User other = user("other");
        Book myBook = book("내가 남긴 책");
        Book othersBook = book("다른 사람이 남긴 책");
        opinion(passage(myBook, me, 1), me);
        opinion(passage(othersBook, other, 1), other);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Long> results = bookQueryRepository.findRecentlyActiveBookIds(me.getId(), pageable);

        assertThat(results.getContent()).containsExactly(myBook.getId());
        assertThat(results.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("내 서재는 내가 흔적을 남긴 도서만 포함하고, ALL 스코프에서는 대목/흔적 수를 도서 전체 기준으로 집계한다")
    void findMyLibraryBooksOnlyIncludesBooksWithMyOpinion() {
        User me = user("me-lib");
        User other = user("other-lib");
        Book myBook = book("내가 흔적을 남긴 책");
        Book othersBook = book("다른 사람만 흔적을 남긴 책");
        Passage myPassage = passage(myBook, me, 1);
        opinion(myPassage, me);
        opinion(myPassage, other);
        opinion(passage(othersBook, other, 1), other);

        Page<BookActivityProjection> results = bookQueryRepository.findMyLibraryBooks(
                me.getId(), PageRequest.of(0, 20), OpinionCountScope.ALL);

        assertThat(results.getContent()).extracting(BookActivityProjection::bookId).containsExactly(myBook.getId());
        assertThat(results.getContent().get(0).passageCount()).isEqualTo(1);
        assertThat(results.getContent().get(0).opinionCount()).isEqualTo(2);
        assertThat(results.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("내가 흔적을 남기지 않은 같은 도서의 다른 대목/흔적도 도서 전체 집계에 포함한다")
    void findMyLibraryBooksCountsWholeBookEvenForPassagesWithoutMyOpinion() {
        User me = user("me-whole");
        User other = user("other-whole");
        Book myBook = book("여러 대목이 있는 책");
        Passage myPassage = passage(myBook, me, 1);
        Passage othersPassage = passage(myBook, other, 2);
        opinion(myPassage, me);
        opinion(othersPassage, other);
        opinion(othersPassage, other);

        Page<BookActivityProjection> results = bookQueryRepository.findMyLibraryBooks(
                me.getId(), PageRequest.of(0, 20), OpinionCountScope.ALL);

        assertThat(results.getContent()).extracting(BookActivityProjection::bookId).containsExactly(myBook.getId());
        assertThat(results.getContent().get(0).passageCount()).isEqualTo(2);
        assertThat(results.getContent().get(0).opinionCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("내 서재는 MINE 스코프에서 흔적 수를 로그인 사용자가 남긴 것만으로 집계한다")
    void findMyLibraryBooksCountsOnlyMyOpinionsWhenScopeIsMine() {
        User me = user("me-mine");
        User other = user("other-mine");
        Book myBook = book("내가 흔적을 남긴 책 - mine scope");
        Passage myPassage = passage(myBook, me, 1);
        opinion(myPassage, me);
        opinion(myPassage, other);
        opinion(myPassage, other);

        Page<BookActivityProjection> results = bookQueryRepository.findMyLibraryBooks(
                me.getId(), PageRequest.of(0, 20), OpinionCountScope.MINE);

        assertThat(results.getContent()).extracting(BookActivityProjection::bookId).containsExactly(myBook.getId());
        assertThat(results.getContent().get(0).passageCount()).isEqualTo(1);
        assertThat(results.getContent().get(0).opinionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("내 서재는 스코프와 무관하게 내가 가장 최근에 흔적을 남긴 도서부터 반환한다")
    void findMyLibraryBooksOrdersByMyMostRecentOpinion() throws InterruptedException {
        User me = user("me-lib-order");
        Book olderBook = book("먼저 흔적을 남긴 책");
        Book newerBook = book("나중에 흔적을 남긴 책");
        opinion(passage(olderBook, me, 1), me);
        Thread.sleep(10);
        opinion(passage(newerBook, me, 1), me);

        Page<BookActivityProjection> results = bookQueryRepository.findMyLibraryBooks(
                me.getId(), PageRequest.of(0, 20), OpinionCountScope.ALL);

        assertThat(results.getContent()).extracting(BookActivityProjection::bookId)
                .containsExactly(newerBook.getId(), olderBook.getId());
    }

    @Test
    @DisplayName("직접 대목을 만들지 않고 기존 대목에 흔적만 남긴 도서도 포함한다")
    void findRecentlyActiveBookIdsIncludesBooksWhereUserOnlyLeftOpinion() {
        User creator = user("creator");
        User me = user("opinion-only");
        Book book = book("다른 사람이 만든 대목에 흔적만 남긴 책");
        Passage existingPassage = passage(book, creator, 1);

        opinion(existingPassage, me);

        Page<Long> results = bookQueryRepository.findRecentlyActiveBookIds(me.getId(), PageRequest.of(0, 10));

        assertThat(results.getContent()).containsExactly(book.getId());
    }
}
