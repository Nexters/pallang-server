package com.nexters.palang.domain.passage.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.ReadingStatus;
import com.nexters.palang.domain.book.domain.UserBookStatus;
import com.nexters.palang.domain.book.infrastructure.UserBookStatusRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.domain.QPassage;
import com.nexters.palang.domain.passage.infrastructure.PassageQueryRepository;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import com.querydsl.core.types.dsl.BooleanExpression;
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

@DataJpaTest
@Import(JpaAuditingConfig.class)
class PassageVisibilityFilterTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserBookStatusRepository userBookStatusRepository;

    private PassageVisibilityFilter passageVisibilityFilter;
    private JPAQueryFactory queryFactory;

    @BeforeEach
    void setUp() {
        queryFactory = new JPAQueryFactory(entityManager.getEntityManager());
        PassageQueryRepository passageQueryRepository = new PassageQueryRepository(queryFactory);
        passageVisibilityFilter = new PassageVisibilityFilter(userBookStatusRepository, passageQueryRepository);
    }

    private User user(String snsId) {
        return entityManager.persistAndFlush(User.builder()
                .nickname("닉네임" + snsId).snsProvider(SnsProvider.KAKAO).snsId(snsId)
                .termsAgreedAt(LocalDateTime.now()).build());
    }

    private Book book() {
        return entityManager.persistAndFlush(Book.builder()
                .title("제목").author("작가").publisher("출판사").pageCount(300).build());
    }

    private Passage passage(Book book, User creator, int pageNumber, boolean isSpoiler) {
        return entityManager.persistAndFlush(Passage.builder()
                .book(book).creator(creator).pageNumber(pageNumber)
                .quotedText("발췌 문장 " + pageNumber).isSpoiler(isSpoiler)
                .normalizedHash("hash-" + pageNumber).build());
    }

    private List<Passage> fetchWithFilter(BooleanExpression filter) {
        QPassage passage = QPassage.passage;
        return queryFactory.selectFrom(passage).where(filter).orderBy(passage.pageNumber.asc()).fetch();
    }

    @Test
    @DisplayName("비로그인 사용자에게는 첫 페이지의 대목만 보인다 (스포일러여도 페이지 자체는 노출 대상에 포함된다)")
    void anonymousUserSeesOnlyFirstPageEvenIfItIsSpoiler() {
        User writer = user("writer-1");
        Book book = book();
        Passage firstVisible = passage(book, writer, 1, true);
        passage(book, writer, 3, false);
        passage(book, writer, 5, false);

        List<Passage> visible = fetchWithFilter(passageVisibilityFilter.build(book.getId(), null));

        assertThat(visible).extracting(Passage::getId).containsExactly(firstVisible.getId());
    }

    @Test
    @DisplayName("읽기상태가 없는 로그인 사용자도 첫 페이지만 보인다 (PLANNED와 동일 취급)")
    void loggedInUserWithoutStatusSeesOnlyFirstPage() {
        User writer = user("writer-2");
        User reader = user("reader-1");
        Book book = book();
        Passage firstVisible = passage(book, writer, 2, false);
        passage(book, writer, 4, false);

        List<Passage> visible = fetchWithFilter(passageVisibilityFilter.build(book.getId(), reader.getId()));

        assertThat(visible).extracting(Passage::getId).containsExactly(firstVisible.getId());
    }

    @Test
    @DisplayName("PLANNED 상태의 사용자는 첫 페이지만 볼 수 있다")
    void plannedUserSeesOnlyFirstPage() {
        User writer = user("writer-3");
        User reader = user("reader-2");
        Book book = book();
        Passage firstVisible = passage(book, writer, 2, false);
        passage(book, writer, 4, false);
        entityManager.persistAndFlush(UserBookStatus.builder()
                .user(reader).book(book).status(ReadingStatus.PLANNED).build());

        List<Passage> visible = fetchWithFilter(passageVisibilityFilter.build(book.getId(), reader.getId()));

        assertThat(visible).extracting(Passage::getId).containsExactly(firstVisible.getId());
    }

    @Test
    @DisplayName("READING 상태의 사용자는 현재 페이지까지의 대목을 볼 수 있다")
    void readingUserSeesUpToCurrentPage() {
        User writer = user("writer-4");
        User reader = user("reader-3");
        Book book = book();
        Passage page2 = passage(book, writer, 2, false);
        Passage page4 = passage(book, writer, 4, false);
        passage(book, writer, 6, false);
        entityManager.persistAndFlush(UserBookStatus.builder()
                .user(reader).book(book).status(ReadingStatus.READING).currentPage(4).build());

        List<Passage> visible = fetchWithFilter(passageVisibilityFilter.build(book.getId(), reader.getId()));

        assertThat(visible).extracting(Passage::getId).containsExactly(page2.getId(), page4.getId());
    }

    @Test
    @DisplayName("READING 상태인데 현재 페이지가 설정되지 않았으면 첫 페이지만 보인다")
    void readingUserWithoutCurrentPageSeesOnlyFirstPage() {
        User writer = user("writer-5");
        User reader = user("reader-4");
        Book book = book();
        Passage firstVisible = passage(book, writer, 2, false);
        passage(book, writer, 4, false);
        entityManager.persistAndFlush(UserBookStatus.builder()
                .user(reader).book(book).status(ReadingStatus.READING).build());

        List<Passage> visible = fetchWithFilter(passageVisibilityFilter.build(book.getId(), reader.getId()));

        assertThat(visible).extracting(Passage::getId).containsExactly(firstVisible.getId());
    }

    @Test
    @DisplayName("스포일러로 표기된 대목도 읽기상태 범위 안이면 노출 대상에 포함된다 (내용 마스킹은 응답 단계의 책임)")
    void spoilerPassagesAreIncludedWhenWithinVisibleRange() {
        User writer = user("writer-6");
        User reader = user("reader-5");
        Book book = book();
        Passage spoiler = passage(book, writer, 2, true);
        Passage nonSpoiler = passage(book, writer, 4, false);
        entityManager.persistAndFlush(UserBookStatus.builder()
                .user(reader).book(book).status(ReadingStatus.READING).currentPage(10).build());

        List<Passage> visible = fetchWithFilter(passageVisibilityFilter.build(book.getId(), reader.getId()));

        assertThat(visible).extracting(Passage::getId).containsExactly(spoiler.getId(), nonSpoiler.getId());
    }

    @Test
    @DisplayName("노출 가능한 대목이 하나도 없으면 첫 노출 페이지는 empty다")
    void firstVisiblePageNumberIsEmptyWhenNoPassages() {
        Book book = book();

        assertThat(passageVisibilityFilter.firstVisiblePageNumber(book.getId())).isEmpty();
    }
}
