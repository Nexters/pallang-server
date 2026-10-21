package com.nexters.palang.domain.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.nexters.palang.domain.book.application.BookActivityProjection;
import com.nexters.palang.domain.book.application.BookDetailProjection;
import com.nexters.palang.domain.book.application.BookSearchProjection;
import com.nexters.palang.domain.book.application.BookSearchSort;
import com.nexters.palang.domain.book.application.MyLibraryBookProjection;
import com.nexters.palang.domain.book.application.OpinionCountScope;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.ReadingStatus;
import com.nexters.palang.domain.book.domain.UserBookStatus;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberRole;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
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

    private Group group(Book book, User host, String name) {
        return entityManager.persistAndFlush(
                Group.create(name, book, host, 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20)));
    }

    private void member(Group group, User user) {
        entityManager.persistAndFlush(GroupMember.of(group, user, GroupMemberRole.MEMBER));
    }

    private Passage groupPassage(Book book, User creator, Group group, int pageNumber) {
        return entityManager.persistAndFlush(Passage.builder()
                .book(book)
                .creator(creator)
                .group(group)
                .pageNumber(pageNumber)
                .quotedText("모임 발췌 문장")
                .isSpoiler(false)
                .normalizedHash("group-hash-" + group.getId() + "-" + pageNumber)
                .build());
    }

    private UserBookStatus userBookStatus(User user, Book book, ReadingStatus status) {
        return entityManager.persistAndFlush(UserBookStatus.builder()
                .user(user)
                .book(book)
                .status(status)
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
    @DisplayName("대목(Passage)이 없는 도서는 검색 결과에서 제외되지만, 흔적(Opinion) 없이 대목만 있는 도서는 포함된다")
    void searchByTitleExcludesBooksWithoutPassages() {
        User writer = user("wsr2");
        Book bookWithoutPassage = book("대목도 없는 책");
        Book bookWithoutOpinion = book("대목만 있는 책");
        passage(bookWithoutOpinion, writer, 1);

        Page<BookSearchProjection> results = bookQueryRepository.searchByTitle("책", BookSearchSort.RECENT,
                PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(BookSearchProjection::bookId)
                .containsExactly(bookWithoutOpinion.getId());
        assertThat(results.getContent()).extracting(BookSearchProjection::passageCount, BookSearchProjection::opinionCount)
                .containsExactly(tuple(1L, 0L));
    }

    @Test
    @DisplayName("흔적이 없는 대목도 도서의 passageCount 집계에 포함한다")
    void searchByTitleCountsPassagesWithoutOpinionsToo() {
        User writer = user("wsr-cnt");
        Book book = book("여러 대목 검색용 책");
        Passage withOpinion = passage(book, writer, 1);
        passage(book, writer, 2); // 흔적 없는 대목
        opinion(withOpinion, writer);

        Page<BookSearchProjection> results = bookQueryRepository.searchByTitle(
                "여러 대목", BookSearchSort.RECENT, PageRequest.of(0, 20));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).passageCount()).isEqualTo(2);
        assertThat(results.getContent().get(0).opinionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("publicPassageCount는 전역 공개 대목만 세고, 모임 전용 대목은 passageCount에만 포함한다")
    void searchByTitleCountsOnlyGlobalPassagesAsPublic() {
        User host = user("wsr-pub");
        Book groupOnlyBook = book("공개 집계 모임 전용 책");
        Book mixedBook = book("공개 집계 섞인 책");
        Group groupOnly = group(groupOnlyBook, host, "모임 전용");
        Group mixedGroup = group(mixedBook, host, "섞인 모임");
        groupPassage(groupOnlyBook, host, groupOnly, 1);
        passage(mixedBook, host, 1);
        passage(mixedBook, host, 2);
        groupPassage(mixedBook, host, mixedGroup, 3);

        Page<BookSearchProjection> results = bookQueryRepository.searchByTitle(
                "공개 집계", BookSearchSort.NAME, PageRequest.of(0, 20));

        assertThat(results.getContent())
                .extracting(BookSearchProjection::bookId, BookSearchProjection::passageCount,
                        BookSearchProjection::publicPassageCount)
                .containsExactlyInAnyOrder(
                        tuple(groupOnlyBook.getId(), 1L, 0L),
                        tuple(mixedBook.getId(), 3L, 2L));
    }

    @Test
    @DisplayName("소프트 삭제된 대목은 passageCount 집계에서 제외된다")
    void searchByTitleExcludesDeletedPassageFromCount() {
        User writer = user("wsr-del");
        Book book = book("삭제된 대목이 있는 책");
        Passage withOpinion = passage(book, writer, 1);
        opinion(withOpinion, writer);
        // 대목은 그 대목의 마지막 흔적이 삭제될 때만 함께 삭제되므로(PM 요구사항), 흔적도 함께 삭제된 상태를 만든다.
        Passage deleted = passage(book, writer, 2);
        Opinion deletedOpinion = opinion(deleted, writer);
        deletedOpinion.delete();
        deleted.delete();
        entityManager.persistAndFlush(deleted);

        Page<BookSearchProjection> results = bookQueryRepository.searchByTitle(
                "삭제된 대목", BookSearchSort.RECENT, PageRequest.of(0, 20));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).passageCount()).isEqualTo(1);
        assertThat(results.getContent().get(0).opinionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("검색어가 빈 문자열이면 대목이 있는 도서 전체 목록을 반환하고, 대목이 없는 도서는 제외한다")
    void searchByTitleReturnsAllBooksWithPassagesWhenKeywordIsBlank() {
        User writer = user("wsr3");
        Book book1 = book("책1");
        Book book2 = book("책2");
        Book bookWithoutPassage = book("책3");
        opinion(passage(book1, writer, 1), writer);
        opinion(passage(book2, writer, 1), writer);

        Page<BookSearchProjection> results = bookQueryRepository.searchByTitle(
                "", BookSearchSort.RECENT, PageRequest.of(0, 20));

        assertThat(results.getTotalElements()).isEqualTo(2);
        assertThat(results.getContent()).extracting(BookSearchProjection::bookId)
                .doesNotContain(bookWithoutPassage.getId());
    }

    @Test
    @DisplayName("정렬 기준과 무관하게 검색어로 시작하는 제목이 그렇지 않은 제목보다 먼저 나온다")
    void searchByTitlePrioritizesPrefixMatchOverSortCriteria() {
        User writer = user("wsr-prefix");
        // "책" 정렬 기준(NAME)으로는 "가나책"이 "책모음"보다 먼저 와야 정상이지만,
        // 검색어 "책"으로 시작하는 "책모음"이 접두어 일치이므로 먼저 나와야 한다.
        Book containsMatch = book("가나책");
        Book prefixMatch = book("책모음");
        opinion(passage(containsMatch, writer, 1), writer);
        opinion(passage(prefixMatch, writer, 1), writer);

        Page<BookSearchProjection> results = bookQueryRepository.searchByTitle(
                "책", BookSearchSort.NAME, PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(BookSearchProjection::bookId)
                .containsExactly(prefixMatch.getId(), containsMatch.getId());
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
    @DisplayName("offset/limit을 직접 주면 Pageable의 page*size로는 표현할 수 없는 임의 위치부터 조회한다")
    void searchByTitleWithRawOffsetAndLimitReturnsArbitrarySlice() {
        User writer = user("wsr-offset");
        Book book1 = book("오프셋 책1");
        Book book2 = book("오프셋 책2");
        Book book3 = book("오프셋 책3");
        opinion(passage(book1, writer, 1), writer);
        opinion(passage(book2, writer, 1), writer);
        opinion(passage(book3, writer, 1), writer);

        List<BookSearchProjection> all = bookQueryRepository.searchByTitle(
                "오프셋", BookSearchSort.NAME, 0L, 10);
        List<BookSearchProjection> middleOnly = bookQueryRepository.searchByTitle(
                "오프셋", BookSearchSort.NAME, 1L, 1);

        assertThat(middleOnly).extracting(BookSearchProjection::bookId)
                .containsExactly(all.get(1).bookId());
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
    @DisplayName("인기 도서와 캐러셀도 publicPassageCount에 전역 공개 대목만 센다")
    void findPopularAndCarouselBooksCountOnlyGlobalPassagesAsPublic() {
        User host = user("writer-pub");
        Book groupOnlyBook = book("모임에서만 읽는 책");
        Book mixedBook = book("전역과 모임에서 읽는 책");
        Group groupOnly = group(groupOnlyBook, host, "모임 전용");
        Group mixedGroup = group(mixedBook, host, "섞인 모임");
        opinion(groupPassage(groupOnlyBook, host, groupOnly, 1), host);
        opinion(passage(mixedBook, host, 1), host);
        groupPassage(mixedBook, host, mixedGroup, 2);

        Page<BookActivityProjection> popular = bookQueryRepository.findPopularBooks(PageRequest.of(0, 10));
        List<BookActivityProjection> carousel = bookQueryRepository.findCarouselBooks(0, 20);

        assertThat(popular.getContent())
                .extracting(BookActivityProjection::bookId, BookActivityProjection::passageCount,
                        BookActivityProjection::publicPassageCount)
                .containsExactlyInAnyOrder(
                        tuple(groupOnlyBook.getId(), 1L, 0L),
                        tuple(mixedBook.getId(), 2L, 1L));
        assertThat(carousel)
                .extracting(BookActivityProjection::bookId, BookActivityProjection::publicPassageCount)
                .containsExactlyInAnyOrder(
                        tuple(groupOnlyBook.getId(), 0L),
                        tuple(mixedBook.getId(), 1L));
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
        Page<Long> results = bookQueryRepository.findRecentlyActiveBookIds(me.getId(), null, pageable);

        assertThat(results.getContent()).containsExactly(myBook.getId());
        assertThat(results.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("keyword로 최근 흔적을 남긴 도서 제목을 필터링한다 (띄어쓰기 무시)")
    void findRecentlyActiveBookIdsFiltersByKeyword() {
        User me = user("me-keyword");
        Book matchingBook = book("작별 인사");
        Book otherBook = book("불편한 편의점");
        opinion(passage(matchingBook, me, 1), me);
        opinion(passage(otherBook, me, 1), me);

        Page<Long> results = bookQueryRepository.findRecentlyActiveBookIds(me.getId(), "작별인사", PageRequest.of(0, 10));

        assertThat(results.getContent()).containsExactly(matchingBook.getId());
        assertThat(results.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("keyword가 빈 문자열이면 필터링 없이 전체 목록을 반환한다")
    void findRecentlyActiveBookIdsReturnsAllWhenKeywordBlank() {
        User me = user("me-kw-blank");
        Book book1 = book("작별 인사");
        Book book2 = book("불편한 편의점");
        opinion(passage(book1, me, 1), me);
        opinion(passage(book2, me, 1), me);

        Page<Long> results = bookQueryRepository.findRecentlyActiveBookIds(me.getId(), "", PageRequest.of(0, 10));

        assertThat(results.getContent()).containsExactlyInAnyOrder(book1.getId(), book2.getId());
        assertThat(results.getTotalElements()).isEqualTo(2);
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

        Page<MyLibraryBookProjection> results = bookQueryRepository.findMyLibraryBooks(
                me.getId(), PageRequest.of(0, 20), OpinionCountScope.ALL);

        assertThat(results.getContent()).extracting(MyLibraryBookProjection::bookId).containsExactly(myBook.getId());
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

        Page<MyLibraryBookProjection> results = bookQueryRepository.findMyLibraryBooks(
                me.getId(), PageRequest.of(0, 20), OpinionCountScope.ALL);

        assertThat(results.getContent()).extracting(MyLibraryBookProjection::bookId).containsExactly(myBook.getId());
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

        Page<MyLibraryBookProjection> results = bookQueryRepository.findMyLibraryBooks(
                me.getId(), PageRequest.of(0, 20), OpinionCountScope.MINE);

        assertThat(results.getContent()).extracting(MyLibraryBookProjection::bookId).containsExactly(myBook.getId());
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

        Page<MyLibraryBookProjection> results = bookQueryRepository.findMyLibraryBooks(
                me.getId(), PageRequest.of(0, 20), OpinionCountScope.ALL);

        assertThat(results.getContent()).extracting(MyLibraryBookProjection::bookId)
                .containsExactly(newerBook.getId(), olderBook.getId());
    }

    @Test
    @DisplayName("흔적은 없지만 읽기 상태만 설정한 도서도 내 서재에 포함한다")
    void findMyLibraryBooksIncludesBookWithStatusOnly() {
        User me = user("me-status1");
        Book statusOnlyBook = book("읽기 상태만 설정한 책");
        userBookStatus(me, statusOnlyBook, ReadingStatus.READING);

        Page<MyLibraryBookProjection> results = bookQueryRepository.findMyLibraryBooks(
                me.getId(), PageRequest.of(0, 20), OpinionCountScope.ALL);

        assertThat(results.getContent()).extracting(MyLibraryBookProjection::bookId)
                .containsExactly(statusOnlyBook.getId());
        assertThat(results.getContent().get(0).passageCount()).isEqualTo(0);
        assertThat(results.getContent().get(0).opinionCount()).isEqualTo(0);
        assertThat(results.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 도서에 흔적과 읽기 상태가 모두 있어도 한 번만 포함한다")
    void findMyLibraryBooksDeduplicatesBookWithBothOpinionAndStatus() {
        User me = user("me-both");
        Book book = book("흔적과 읽기 상태가 모두 있는 책");
        opinion(passage(book, me, 1), me);
        userBookStatus(me, book, ReadingStatus.FINISHED);

        Page<MyLibraryBookProjection> results = bookQueryRepository.findMyLibraryBooks(
                me.getId(), PageRequest.of(0, 20), OpinionCountScope.ALL);

        assertThat(results.getContent()).extracting(MyLibraryBookProjection::bookId).containsExactly(book.getId());
        assertThat(results.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 책을 전역과 모임에서 함께 남기면 (책, 모임) 단위 항목으로 나누고, 대목/흔적 수도 항목 범위 안에서만 센다")
    void findMyLibraryBooksSplitsEntriesByGroup() {
        // given
        User me = user("me-split");
        User other = user("other-split");
        Book book = book("면도날");
        Group myGroup = group(book, me, "면도날 모임");
        member(myGroup, me);
        Group othersGroup = group(book, other, "남의 모임");
        member(othersGroup, other);

        Passage globalPassage = passage(book, me, 1);
        opinion(globalPassage, me);
        opinion(globalPassage, other);
        opinion(passage(book, other, 2), other);
        opinion(groupPassage(book, me, myGroup, 9), me);
        Passage othersGroupPassage = groupPassage(book, other, othersGroup, 10);
        opinion(othersGroupPassage, other);
        opinion(othersGroupPassage, other);

        // when
        Page<MyLibraryBookProjection> results = bookQueryRepository.findMyLibraryBooks(
                me.getId(), PageRequest.of(0, 20), OpinionCountScope.ALL);

        // then
        assertThat(results.getContent())
                .extracting(MyLibraryBookProjection::bookId, MyLibraryBookProjection::groupId,
                        MyLibraryBookProjection::groupName, MyLibraryBookProjection::passageCount,
                        MyLibraryBookProjection::opinionCount)
                .containsExactlyInAnyOrder(
                        tuple(book.getId(), null, null, 2L, 3L),
                        tuple(book.getId(), myGroup.getId(), "면도날 모임", 1L, 1L));
        assertThat(results.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("모임 항목은 MINE 스코프에서 그 모임 안에서 내가 남긴 흔적만 센다")
    void findMyLibraryBooksCountsOnlyMyOpinionsInGroupWhenScopeIsMine() {
        // given
        User me = user("me-gmine");
        User other = user("other-gmine");
        Book book = book("모임 책 - mine scope");
        Group group = group(book, me, "모임");
        member(group, me);
        member(group, other);
        Passage passage = groupPassage(book, me, group, 3);
        opinion(passage, me);
        opinion(passage, other);
        opinion(passage(book, me, 1), me);

        // when
        Page<MyLibraryBookProjection> results = bookQueryRepository.findMyLibraryBooks(
                me.getId(), PageRequest.of(0, 20), OpinionCountScope.MINE);

        // then
        assertThat(results.getContent())
                .extracting(MyLibraryBookProjection::groupId, MyLibraryBookProjection::opinionCount)
                .containsExactlyInAnyOrder(tuple(null, 1L), tuple(group.getId(), 1L));
    }

    @Test
    @DisplayName("내가 멤버가 아닌 모임의 대목에 남은 흔적은 내 서재에 포함하지 않는다")
    void findMyLibraryBooksExcludesGroupsImNotMemberOf() {
        // given
        User me = user("me-nomem");
        User host = user("host-nomem");
        Book book = book("멤버가 아닌 모임의 책");
        Group group = group(book, host, "모임");
        member(group, host);
        opinion(groupPassage(book, host, group, 1), me);

        // when
        Page<MyLibraryBookProjection> results = bookQueryRepository.findMyLibraryBooks(
                me.getId(), PageRequest.of(0, 20), OpinionCountScope.ALL);

        // then
        assertThat(results.getContent()).isEmpty();
        assertThat(results.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("모임에서만 흔적을 남긴 책에 읽기 상태를 설정해도 별도 전역 항목을 만들지 않는다")
    void findMyLibraryBooksAttachesStatusToGroupEntryWhenNoGlobalActivity() {
        // given
        User me = user("me-gstat");
        Book book = book("모임에서만 읽는 책");
        Group group = group(book, me, "모임");
        member(group, me);
        opinion(groupPassage(book, me, group, 1), me);
        userBookStatus(me, book, ReadingStatus.READING);

        // when
        Page<MyLibraryBookProjection> results = bookQueryRepository.findMyLibraryBooks(
                me.getId(), PageRequest.of(0, 20), OpinionCountScope.ALL);

        // then
        assertThat(results.getContent()).extracting(MyLibraryBookProjection::groupId)
                .containsExactly(group.getId());
        assertThat(results.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("직접 대목을 만들지 않고 기존 대목에 흔적만 남긴 도서도 포함한다")
    void findRecentlyActiveBookIdsIncludesBooksWhereUserOnlyLeftOpinion() {
        User creator = user("creator");
        User me = user("opinion-only");
        Book book = book("다른 사람이 만든 대목에 흔적만 남긴 책");
        Passage existingPassage = passage(book, creator, 1);

        opinion(existingPassage, me);

        Page<Long> results = bookQueryRepository.findRecentlyActiveBookIds(me.getId(), null, PageRequest.of(0, 10));

        assertThat(results.getContent()).containsExactly(book.getId());
    }

    @Test
    @DisplayName("도서 상세는 대목/흔적 유무와 무관하게 도서 메타와 집계 수를 함께 반환한다")
    void findBookDetailReturnsBookWithCounts() {
        User writer = user("wsr-dtl");
        Book book = book("상세 조회용 책");
        Passage passage1 = passage(book, writer, 1);
        opinion(passage1, writer);
        opinion(passage1, writer);

        BookDetailProjection result = bookQueryRepository.findBookDetail(book.getId()).orElseThrow();

        assertThat(result.bookId()).isEqualTo(book.getId());
        assertThat(result.passageCount()).isEqualTo(1);
        assertThat(result.opinionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("대목이 없는 도서도 도서 상세 조회 대상에 포함되고 집계 수는 0이다")
    void findBookDetailIncludesBookWithoutPassages() {
        Book book = book("대목 없는 책");

        BookDetailProjection result = bookQueryRepository.findBookDetail(book.getId()).orElseThrow();

        assertThat(result.passageCount()).isEqualTo(0);
        assertThat(result.opinionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 도서를 조회하면 빈 Optional을 반환한다")
    void findBookDetailReturnsEmptyWhenBookNotFound() {
        assertThat(bookQueryRepository.findBookDetail(999L)).isEmpty();
    }
}
