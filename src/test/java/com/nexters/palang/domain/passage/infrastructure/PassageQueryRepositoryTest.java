package com.nexters.palang.domain.passage.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.passage.application.SimilarPassageProjection;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.domain.QPassage;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class PassageQueryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    private PassageQueryRepository passageQueryRepository;

    @BeforeEach
    void setUp() {
        passageQueryRepository = new PassageQueryRepository(new JPAQueryFactory(entityManager.getEntityManager()));
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

    private Passage passage(Book book, User creator, int pageNumber, String quotedText, String normalizedHash) {
        return entityManager.persistAndFlush(Passage.builder()
                .book(book)
                .creator(creator)
                .pageNumber(pageNumber)
                .quotedText(quotedText)
                .isSpoiler(false)
                .normalizedHash(normalizedHash)
                .build());
    }

    private Passage passage(Book book, User creator, int pageNumber, boolean isSpoiler) {
        return entityManager.persistAndFlush(Passage.builder()
                .book(book)
                .creator(creator)
                .pageNumber(pageNumber)
                .quotedText("발췌 문장 " + pageNumber)
                .isSpoiler(isSpoiler)
                .normalizedHash("hash-" + book.getId() + "-" + pageNumber)
                .build());
    }

    private static final BooleanExpression NOT_SPOILER = QPassage.passage.isSpoiler.isFalse();

    @Test
    @DisplayName("같은 책의 인접 페이지(±1)에서 정규화 해시가 같은 대목을 후보로 조회한다")
    void findSimilarCandidatesReturnsPassagesWithinAdjacentPages() {
        User writer = user("writer-1");
        Book book = book("책");
        Passage samePage = passage(book, writer, 5, "발췌 문장", "hash-1");
        Passage adjacentPage = passage(book, writer, 6, "발췌 문장", "hash-1");
        passage(book, writer, 8, "먼 페이지 문장", "hash-1");

        List<SimilarPassageProjection> results = passageQueryRepository.findSimilarCandidates(book.getId(), 5, "hash-1");

        assertThat(results).extracting(SimilarPassageProjection::passageId)
                .containsExactly(samePage.getId(), adjacentPage.getId());
    }

    @Test
    @DisplayName("정규화 해시가 다르면 후보에서 제외된다")
    void findSimilarCandidatesExcludesDifferentHash() {
        User writer = user("writer-2");
        Book book = book("책");
        passage(book, writer, 5, "다른 문장", "hash-different");

        List<SimilarPassageProjection> results = passageQueryRepository.findSimilarCandidates(book.getId(), 5, "hash-1");

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("같은 대목에 연결된 흔적 수를 함께 반환한다")
    void findSimilarCandidatesIncludesOpinionCount() {
        User writer = user("writer-3");
        Book book = book("책");
        Passage passage = passage(book, writer, 5, "발췌 문장", "hash-1");
        entityManager.persistAndFlush(Opinion.builder().passage(passage).user(writer).content("흔적1").build());
        entityManager.persistAndFlush(Opinion.builder().passage(passage).user(writer).content("흔적2").build());

        List<SimilarPassageProjection> results = passageQueryRepository.findSimilarCandidates(book.getId(), 5, "hash-1");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).opinionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("스포일러가 아닌 대목 중 가장 작은 페이지 번호를 첫 노출 페이지로 조회한다")
    void findFirstVisiblePageNumberIgnoresSpoilerPassages() {
        User writer = user("writer-4");
        Book book = book("책");
        passage(book, writer, 1, true);
        passage(book, writer, 3, false);
        passage(book, writer, 5, false);

        Integer firstPage = passageQueryRepository.findFirstVisiblePageNumber(book.getId());

        assertThat(firstPage).isEqualTo(3);
    }

    @Test
    @DisplayName("노출 대상 대목이 하나도 없으면 첫 노출 페이지는 null이다")
    void findFirstVisiblePageNumberReturnsNullWhenNoPassages() {
        Book book = book("빈 책");

        Integer firstPage = passageQueryRepository.findFirstVisiblePageNumber(book.getId());

        assertThat(firstPage).isNull();
    }

    @Test
    @DisplayName("노출 필터를 만족하는 서로 다른 페이지 번호만 오름차순으로 조회한다")
    void findVisiblePageNumbersReturnsDistinctPagesInAscendingOrder() {
        User writer = user("writer-5");
        Book book = book("책");
        passage(book, writer, 5, false);
        passage(book, writer, 5, false);
        passage(book, writer, 2, false);
        passage(book, writer, 1, true);

        Page<Integer> result = passageQueryRepository.findVisiblePageNumbers(book.getId(), NOT_SPOILER, PageRequest.of(0, 10));

        assertThat(result.getContent()).containsExactly(2, 5);
    }

    @Test
    @DisplayName("특정 페이지에 걸친 대목을 노출 필터에 맞춰 등록 순으로 조회한다")
    void findVisiblePassagesByPageReturnsPassagesInCreationOrder() {
        User writer = user("writer-6");
        Book book = book("책");
        Passage first = passage(book, writer, 3, false);
        Passage second = passage(book, writer, 3, false);
        passage(book, writer, 3, true);

        List<Passage> result = passageQueryRepository.findVisiblePassagesByPage(book.getId(), 3, NOT_SPOILER);

        assertThat(result).extracting(Passage::getId).containsExactly(first.getId(), second.getId());
    }
}
