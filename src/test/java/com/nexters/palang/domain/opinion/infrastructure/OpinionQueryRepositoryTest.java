package com.nexters.palang.domain.opinion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.opinion.application.OpinionSummaryProjection;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.domain.OpinionSortType;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class OpinionQueryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    private OpinionQueryRepository opinionQueryRepository;

    @BeforeEach
    void setUp() {
        opinionQueryRepository = new OpinionQueryRepository(new JPAQueryFactory(entityManager.getEntityManager()));
    }

    private User user(String snsId) {
        return entityManager.persistAndFlush(User.builder()
                .nickname("닉네임" + snsId).snsProvider(SnsProvider.KAKAO).snsId(snsId)
                .termsAgreedAt(LocalDateTime.now()).build());
    }

    private Passage passage(User creator) {
        Book book = entityManager.persistAndFlush(Book.builder()
                .title("제목").author("작가").publisher("출판사").pageCount(300).build());
        return entityManager.persistAndFlush(Passage.builder()
                .book(book).creator(creator).pageNumber(1).quotedText("발췌 문장").isSpoiler(false)
                .normalizedHash("hash").build());
    }

    private Opinion opinion(Passage passage, User writer, String content, int likeCount) {
        Opinion opinion = Opinion.builder().passage(passage).user(writer).content(content).build();
        ReflectionTestUtils.setField(opinion, "likeCount", likeCount);
        return entityManager.persistAndFlush(opinion);
    }

    @Test
    @DisplayName("정렬 기준을 좋아요순으로 주면 좋아요가 많은 흔적부터 조회한다")
    void findOpinionsOrdersByLikeCountWhenSortTypeIsLikes() {
        User writer = user("writer-1");
        Passage passage = passage(writer);
        opinion(passage, writer, "적은 좋아요", 1);
        Opinion popular = opinion(passage, writer, "많은 좋아요", 10);

        Page<OpinionSummaryProjection> result = opinionQueryRepository.findOpinions(
                passage.getId(), OpinionSortType.LIKES, PageRequest.of(0, 10));

        assertThat(result.getContent().get(0).opinionId()).isEqualTo(popular.getId());
    }

    @Test
    @DisplayName("정렬 기준을 주지 않으면(최신순) 나중에 작성된 흔적부터 조회한다")
    void findOpinionsOrdersByCreatedAtDescendingByDefault() {
        User writer = user("writer-2");
        Passage passage = passage(writer);
        opinion(passage, writer, "먼저 작성", 5);
        Opinion later = opinion(passage, writer, "나중에 작성", 1);

        Page<OpinionSummaryProjection> result = opinionQueryRepository.findOpinions(
                passage.getId(), OpinionSortType.LATEST, PageRequest.of(0, 10));

        assertThat(result.getContent().get(0).opinionId()).isEqualTo(later.getId());
    }

    @Test
    @DisplayName("삭제된 흔적은 목록에서 제외된다")
    void findOpinionsExcludesDeletedOpinions() {
        User writer = user("writer-3");
        Passage passage = passage(writer);
        Opinion active = opinion(passage, writer, "활성 흔적", 0);
        Opinion deleted = opinion(passage, writer, "삭제된 흔적", 0);
        deleted.delete();
        entityManager.persistAndFlush(deleted);

        Page<OpinionSummaryProjection> result = opinionQueryRepository.findOpinions(
                passage.getId(), OpinionSortType.LATEST, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(OpinionSummaryProjection::opinionId)
                .containsExactly(active.getId());
    }

    @Test
    @DisplayName("작성자의 닉네임을 함께 조회한다")
    void findOpinionsIncludesAuthorNickname() {
        User writer = user("writer-4");
        Passage passage = passage(writer);
        opinion(passage, writer, "흔적 내용", 0);

        Page<OpinionSummaryProjection> result = opinionQueryRepository.findOpinions(
                passage.getId(), OpinionSortType.LATEST, PageRequest.of(0, 10));

        assertThat(result.getContent().get(0).nickname()).isEqualTo(writer.getNickname());
    }
}
