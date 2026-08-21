package com.nexters.palang.domain.passage.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.application.BookOptionProjection;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.passage.application.MyPassageProjection;
import com.nexters.palang.domain.passage.application.SimilarPassageProjection;
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

    private Group group(Book book, User host) {
        Group built = Group.create("모임", book, host, 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));
        return entityManager.persistAndFlush(built);
    }

    private Passage passage(Book book, User creator, Group group, int pageNumber, String quotedText, String normalizedHash) {
        return entityManager.persistAndFlush(Passage.builder()
                .book(book)
                .creator(creator)
                .group(group)
                .pageNumber(pageNumber)
                .quotedText(quotedText)
                .isSpoiler(false)
                .normalizedHash(normalizedHash)
                .build());
    }

    @Test
    @DisplayName("같은 책의 인접 페이지(±1)에서 정규화 해시가 같은 대목을 후보로 조회한다")
    void findSimilarCandidatesReturnsPassagesWithinAdjacentPages() {
        User writer = user("writer-1");
        Book book = book("책");
        Passage samePage = passage(book, writer, 5, "발췌 문장", "hash-1");
        Passage adjacentPage = passage(book, writer, 6, "발췌 문장", "hash-1");
        passage(book, writer, 8, "먼 페이지 문장", "hash-1");

        List<SimilarPassageProjection> results = passageQueryRepository.findSimilarCandidates(book.getId(), 5, "hash-1", null);

        assertThat(results).extracting(SimilarPassageProjection::passageId)
                .containsExactly(samePage.getId(), adjacentPage.getId());
    }

    @Test
    @DisplayName("정규화 해시가 다르면 후보에서 제외된다")
    void findSimilarCandidatesExcludesDifferentHash() {
        User writer = user("writer-2");
        Book book = book("책");
        passage(book, writer, 5, "다른 문장", "hash-different");

        List<SimilarPassageProjection> results = passageQueryRepository.findSimilarCandidates(book.getId(), 5, "hash-1", null);

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

        List<SimilarPassageProjection> results = passageQueryRepository.findSimilarCandidates(book.getId(), 5, "hash-1", null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).opinionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("소프트 삭제된 대목은 유사 후보에서 제외된다")
    void findSimilarCandidatesExcludesDeletedPassage() {
        User writer = user("writer-4");
        Book book = book("책");
        Passage deleted = passage(book, writer, 5, "발췌 문장", "hash-1");
        deleted.delete();
        entityManager.persistAndFlush(deleted);

        List<SimilarPassageProjection> results = passageQueryRepository.findSimilarCandidates(book.getId(), 5, "hash-1", null);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("같은 해시라도 모임이 다르면(또는 한쪽이 전역 공개면) 유사 후보로 묶이지 않는다")
    void findSimilarCandidatesScopesByGroup() {
        User writer = user("writer-13");
        Book book = book("책");
        Group group = group(book, writer);
        Passage global = passage(book, writer, 5, "발췌 문장", "hash-1");
        Passage inGroup = passage(book, writer, group, 5, "발췌 문장", "hash-1");

        List<SimilarPassageProjection> globalResults = passageQueryRepository.findSimilarCandidates(book.getId(), 5, "hash-1", null);
        List<SimilarPassageProjection> groupResults = passageQueryRepository.findSimilarCandidates(book.getId(), 5, "hash-1", group.getId());

        assertThat(globalResults).extracting(SimilarPassageProjection::passageId).containsExactly(global.getId());
        assertThat(groupResults).extracting(SimilarPassageProjection::passageId).containsExactly(inGroup.getId());
    }

    @Test
    @DisplayName("서로 다른 페이지 번호만 오름차순으로 조회한다 (스포일러 페이지도 포함)")
    void findPageNumbersReturnsDistinctPagesInAscendingOrderIncludingSpoilers() {
        User writer = user("writer-5");
        Book book = book("책");
        passage(book, writer, 5, false);
        passage(book, writer, 5, false);
        passage(book, writer, 2, false);
        passage(book, writer, 1, true);

        Page<Integer> result = passageQueryRepository.findPageNumbers(book.getId(), null, PageRequest.of(0, 10));

        assertThat(result.getContent()).containsExactly(1, 2, 5);
    }

    @Test
    @DisplayName("소프트 삭제된 대목의 페이지 번호는 제외된다")
    void findPageNumbersExcludesDeletedPassage() {
        User writer = user("writer-7");
        Book book = book("책");
        passage(book, writer, 1, false);
        Passage deleted = passage(book, writer, 9, false);
        deleted.delete();
        entityManager.persistAndFlush(deleted);

        Page<Integer> result = passageQueryRepository.findPageNumbers(book.getId(), null, PageRequest.of(0, 10));

        assertThat(result.getContent()).containsExactly(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("책에 대목이 하나도 없으면 빈 결과가 조회된다")
    void findPageNumbersReturnsEmptyWhenNoPassages() {
        Book book = book("빈 책");

        Page<Integer> result = passageQueryRepository.findPageNumbers(book.getId(), null, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("groupId를 지정하면 그 모임 전용 대목의 페이지 번호만 조회된다")
    void findPageNumbersScopesByGroup() {
        User writer = user("writer-14");
        Book book = book("책");
        Group group = group(book, writer);
        passage(book, writer, 1, false);
        passage(book, writer, group, 2, "모임 전용 문장", "hash-group");

        Page<Integer> globalResult = passageQueryRepository.findPageNumbers(book.getId(), null, PageRequest.of(0, 10));
        Page<Integer> groupResult = passageQueryRepository.findPageNumbers(book.getId(), group.getId(), PageRequest.of(0, 10));

        assertThat(globalResult.getContent()).containsExactly(1);
        assertThat(groupResult.getContent()).containsExactly(2);
    }

    @Test
    @DisplayName("특정 페이지에 걸친 대목을 등록 순으로 조회한다 (스포일러 대목도 포함)")
    void findPassagesByPageReturnsPassagesInCreationOrderIncludingSpoilers() {
        User writer = user("writer-6");
        Book book = book("책");
        Passage first = passage(book, writer, 3, false);
        Passage second = passage(book, writer, 3, false);
        Passage spoiler = passage(book, writer, 3, true);

        List<Passage> result = passageQueryRepository.findPassagesByPage(book.getId(), null, 3);

        assertThat(result).extracting(Passage::getId)
                .containsExactly(first.getId(), second.getId(), spoiler.getId());
    }

    @Test
    @DisplayName("소프트 삭제된 대목은 페이지 전환 조회에서 제외된다")
    void findPassagesByPageExcludesDeletedPassage() {
        User writer = user("writer-8");
        Book book = book("책");
        Passage alive = passage(book, writer, 3, false);
        Passage deleted = passage(book, writer, 3, false);
        deleted.delete();
        entityManager.persistAndFlush(deleted);

        List<Passage> result = passageQueryRepository.findPassagesByPage(book.getId(), null, 3);

        assertThat(result).extracting(Passage::getId).containsExactly(alive.getId());
    }

    @Test
    @DisplayName("groupId를 지정하면 그 모임 전용 대목만 조회되고 전역 공개 대목은 섞이지 않는다")
    void findPassagesByPageScopesByGroup() {
        User writer = user("writer-15");
        Book book = book("책");
        Group group = group(book, writer);
        Passage global = passage(book, writer, 3, false);
        Passage inGroup = passage(book, writer, group, 3, "모임 전용 문장", "hash-group");

        List<Passage> globalResult = passageQueryRepository.findPassagesByPage(book.getId(), null, 3);
        List<Passage> groupResult = passageQueryRepository.findPassagesByPage(book.getId(), group.getId(), 3);

        assertThat(globalResult).extracting(Passage::getId).containsExactly(global.getId());
        assertThat(groupResult).extracting(Passage::getId).containsExactly(inGroup.getId());
    }

    @Test
    @DisplayName("병합된 대목은 최초 생성자가 아니어도 흔적을 남긴 사용자에게 노출된다")
    void findMyPassagesIncludesMergedPassageForTraceOwner() {
        User creator = user("creator-1");
        User merger = user("merger-1");
        Book book = book("책");
        Passage passage = passage(book, creator, 5, "발췌 문장", "hash-1");
        entityManager.persistAndFlush(Opinion.builder().passage(passage).user(merger).content("병합 흔적").build());

        Page<MyPassageProjection> result = passageQueryRepository.findMyPassages(
                merger.getId(), null, false, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(MyPassageProjection::passageId).containsExactly(passage.getId());
    }

    @Test
    @DisplayName("흔적을 남긴 적 없는 대목은 조회되지 않는다")
    void findMyPassagesExcludesPassageWithoutMyOpinion() {
        User creator = user("creator-2");
        User other = user("other-2");
        Book book = book("책");
        Passage passage = passage(book, creator, 5, "발췌 문장", "hash-1");
        entityManager.persistAndFlush(Opinion.builder().passage(passage).user(creator).content("흔적").build());

        Page<MyPassageProjection> result = passageQueryRepository.findMyPassages(
                other.getId(), null, false, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("bookId를 지정하면 해당 책의 대목만 조회된다")
    void findMyPassagesFiltersByBookId() {
        User writer = user("writer-9");
        Book targetBook = book("대상 책");
        Book otherBook = book("다른 책");
        Passage inTargetBook = passage(targetBook, writer, 5, "발췌 문장", "hash-1");
        Passage inOtherBook = passage(otherBook, writer, 5, "발췌 문장", "hash-2");
        entityManager.persistAndFlush(Opinion.builder().passage(inTargetBook).user(writer).content("흔적1").build());
        entityManager.persistAndFlush(Opinion.builder().passage(inOtherBook).user(writer).content("흔적2").build());

        Page<MyPassageProjection> result = passageQueryRepository.findMyPassages(
                writer.getId(), targetBook.getId(), false, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(MyPassageProjection::passageId)
                .containsExactly(inTargetBook.getId());
    }

    @Test
    @DisplayName("spoilerOnly=true면 스포일러 대목만 조회된다")
    void findMyPassagesFiltersBySpoilerOnly() {
        User writer = user("writer-10");
        Book book = book("책");
        Passage spoiler = passage(book, writer, 1, true);
        Passage notSpoiler = passage(book, writer, 2, false);
        entityManager.persistAndFlush(Opinion.builder().passage(spoiler).user(writer).content("흔적1").build());
        entityManager.persistAndFlush(Opinion.builder().passage(notSpoiler).user(writer).content("흔적2").build());

        Page<MyPassageProjection> result = passageQueryRepository.findMyPassages(
                writer.getId(), null, true, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(MyPassageProjection::passageId).containsExactly(spoiler.getId());
    }

    @Test
    @DisplayName("같은 대목에 흔적을 여러 번 남겨도 대목은 한 번만 조회된다")
    void findMyPassagesDeduplicatesSamePassage() {
        User writer = user("writer-11");
        Book book = book("책");
        Passage passage = passage(book, writer, 5, "발췌 문장", "hash-1");
        entityManager.persistAndFlush(Opinion.builder().passage(passage).user(writer).content("흔적1").build());
        Opinion latest = entityManager.persistAndFlush(
                Opinion.builder().passage(passage).user(writer).content("흔적2").build());

        Page<MyPassageProjection> result = passageQueryRepository.findMyPassages(
                writer.getId(), null, false, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).opinionId()).isEqualTo(latest.getId());
    }

    @Test
    @DisplayName("스포일러로 흔적을 남긴 도서 목록을 최근 활동순으로 중복 없이 반환한다")
    void findSpoilerBookOptionsReturnsDistinctBooksWithSpoilerOpinion() {
        User writer = user("writer-12");
        Book targetBook = book("대상 책");
        Book noSpoilerBook = book("스포일러 없는 책");
        Passage spoiler = passage(targetBook, writer, 1, true);
        Passage notSpoiler = passage(noSpoilerBook, writer, 1, false);
        entityManager.persistAndFlush(Opinion.builder().passage(spoiler).user(writer).content("흔적1").build());
        entityManager.persistAndFlush(Opinion.builder().passage(notSpoiler).user(writer).content("흔적2").build());

        Page<BookOptionProjection> results = passageQueryRepository.findSpoilerBookOptions(
                writer.getId(), PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(BookOptionProjection::bookId).containsExactly(targetBook.getId());
    }
}
