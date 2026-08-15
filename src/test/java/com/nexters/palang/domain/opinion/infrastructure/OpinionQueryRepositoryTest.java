package com.nexters.palang.domain.opinion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.block.domain.UserBlock;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.opinion.application.LikedOpinionProjection;
import com.nexters.palang.domain.opinion.application.MyOpinionProjection;
import com.nexters.palang.domain.opinion.application.OpinionSummaryProjection;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.domain.OpinionLike;
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

    private User me;
    private User other;
    private Book book;

    @BeforeEach
    void setUp() {
        opinionQueryRepository = new OpinionQueryRepository(new JPAQueryFactory(entityManager.getEntityManager()));

        me = user("me");
        other = user("other");
        book = entityManager.persistAndFlush(Book.builder()
                .title("책").author("작가").publisher("출판사").pageCount(300).build());
    }

    private User user(String snsId) {
        return entityManager.persistAndFlush(User.builder()
                .nickname("닉네임" + snsId)
                .snsProvider(SnsProvider.KAKAO)
                .snsId(snsId)
                .termsAgreedAt(LocalDateTime.now())
                .build());
    }

    private Passage passage(User creator) {
        Book book = entityManager.persistAndFlush(Book.builder()
                .title("제목").author("작가").publisher("출판사").pageCount(300).build());
        return entityManager.persistAndFlush(Passage.builder()
                .book(book).creator(creator).pageNumber(1).quotedText("발췌 문장").isSpoiler(false)
                .normalizedHash("hash").build());
    }

    private Passage passage(int pageNumber) {
        return entityManager.persistAndFlush(Passage.builder()
                .book(book).creator(me).pageNumber(pageNumber).quotedText("발췌 문장").isSpoiler(false)
                .normalizedHash("hash-" + pageNumber).build());
    }

    private Opinion opinion(Passage passage, User writer, String content, int likeCount) {
        Opinion opinion = Opinion.builder().passage(passage).user(writer).content(content).build();
        ReflectionTestUtils.setField(opinion, "likeCount", likeCount);
        return entityManager.persistAndFlush(opinion);
    }

    private Opinion opinion(User user, Passage passage, String content) {
        return entityManager.persistAndFlush(Opinion.builder().passage(passage).user(user).content(content).build());
    }

    @Test
    @DisplayName("정렬 기준을 좋아요순으로 주면 좋아요가 많은 흔적부터 조회한다")
    void findOpinionsOrdersByLikeCountWhenSortTypeIsLikes() {
        User writer = user("writer-1");
        Passage passage = passage(writer);
        opinion(passage, writer, "적은 좋아요", 1);
        Opinion popular = opinion(passage, writer, "많은 좋아요", 10);

        Page<OpinionSummaryProjection> result = opinionQueryRepository.findOpinions(
                passage.getId(), OpinionSortType.LIKES, PageRequest.of(0, 10), null);

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
                passage.getId(), OpinionSortType.LATEST, PageRequest.of(0, 10), null);

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
                passage.getId(), OpinionSortType.LATEST, PageRequest.of(0, 10), null);

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
                passage.getId(), OpinionSortType.LATEST, PageRequest.of(0, 10), null);

        assertThat(result.getContent().get(0).nickname()).isEqualTo(writer.getNickname());
    }

    @Test
    @DisplayName("currentUserId가 좋아요를 눌렀으면 liked=true, 비로그인(null)이면 liked=false를 반환한다")
    void findOpinionsReflectsLikedForCurrentUser() {
        User writer = user("writer-5");
        Passage passage = passage(writer);
        Opinion opinion = opinion(passage, writer, "흔적 내용", 0);
        entityManager.persistAndFlush(OpinionLike.builder().user(me).opinion(opinion).build());

        Page<OpinionSummaryProjection> likedResult = opinionQueryRepository.findOpinions(
                passage.getId(), OpinionSortType.LATEST, PageRequest.of(0, 10), me.getId());
        Page<OpinionSummaryProjection> anonymousResult = opinionQueryRepository.findOpinions(
                passage.getId(), OpinionSortType.LATEST, PageRequest.of(0, 10), null);
        Page<OpinionSummaryProjection> otherResult = opinionQueryRepository.findOpinions(
                passage.getId(), OpinionSortType.LATEST, PageRequest.of(0, 10), other.getId());

        assertThat(likedResult.getContent().get(0).liked()).isTrue();
        assertThat(anonymousResult.getContent().get(0).liked()).isFalse();
        assertThat(otherResult.getContent().get(0).liked()).isFalse();
    }

    @Test
    @DisplayName("commentCount는 답글을 포함하고 삭제된 댓글은 제외한다")
    void findOpinionsCountsCommentsIncludingRepliesExcludingDeleted() {
        User writer = user("writer-6");
        Passage passage = passage(writer);
        Opinion opinion = opinion(passage, writer, "흔적 내용", 0);
        Comment root = entityManager.persistAndFlush(Comment.root(opinion, writer, "원댓글"));
        entityManager.persistAndFlush(Comment.reply(root, writer, "답글"));
        Comment deletedRoot = entityManager.persistAndFlush(Comment.root(opinion, writer, "삭제될 댓글"));
        deletedRoot.delete();
        entityManager.persistAndFlush(deletedRoot);

        Page<OpinionSummaryProjection> result = opinionQueryRepository.findOpinions(
                passage.getId(), OpinionSortType.LATEST, PageRequest.of(0, 10), null);

        assertThat(result.getContent().get(0).commentCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("차단한 작성자의 흔적은 피드에서 제외되고, 차단하지 않은 사용자에게는 그대로 보인다")
    void findOpinionsExcludesOpinionsFromBlockedWriter() {
        User blockedWriter = user("blocked-w");
        Passage passage = passage(blockedWriter);
        Opinion opinionByBlockedWriter = opinion(passage, blockedWriter, "차단한 사람의 흔적", 0);
        entityManager.persistAndFlush(UserBlock.of(me, blockedWriter));

        Page<OpinionSummaryProjection> resultForMe = opinionQueryRepository.findOpinions(
                passage.getId(), OpinionSortType.LATEST, PageRequest.of(0, 10), me.getId());
        Page<OpinionSummaryProjection> resultForOther = opinionQueryRepository.findOpinions(
                passage.getId(), OpinionSortType.LATEST, PageRequest.of(0, 10), other.getId());
        Page<OpinionSummaryProjection> resultForAnonymous = opinionQueryRepository.findOpinions(
                passage.getId(), OpinionSortType.LATEST, PageRequest.of(0, 10), null);

        assertThat(resultForMe.getContent()).isEmpty();
        assertThat(resultForOther.getContent()).extracting(OpinionSummaryProjection::opinionId)
                .containsExactly(opinionByBlockedWriter.getId());
        assertThat(resultForAnonymous.getContent()).extracting(OpinionSummaryProjection::opinionId)
                .containsExactly(opinionByBlockedWriter.getId());
    }

    @Test
    @DisplayName("내가 남긴 흔적만 최신순으로 조회하고 삭제된 흔적/다른 사람 흔적은 제외한다")
    void findMyOpinionsExcludesDeletedAndOtherUsers() {
        Opinion older = opinion(me, passage(1), "첫 흔적");
        Opinion newer = opinion(me, passage(2), "둘째 흔적");
        Opinion deleted = opinion(me, passage(3), "삭제된 흔적");
        deleted.delete();
        opinion(other, passage(4), "다른 사람 흔적");

        Page<MyOpinionProjection> results = opinionQueryRepository.findMyOpinions(me.getId(), PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(MyOpinionProjection::opinionId)
                .containsExactly(newer.getId(), older.getId());
        assertThat(results.getContent()).extracting(MyOpinionProjection::author)
                .containsOnly(book.getAuthor());
        assertThat(results.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("좋아요 누른 흔적을 좋아요 순서(최신순)로 조회하고 삭제된 흔적의 좋아요는 제외한다")
    void findLikedOpinionsOrdersByLikedAtDescendingAndExcludesDeleted() {
        Opinion opinion1 = opinion(other, passage(1), "흔적1");
        Opinion opinion2 = opinion(other, passage(2), "흔적2");
        Opinion deletedOpinion = opinion(other, passage(3), "삭제될 흔적");

        entityManager.persistAndFlush(OpinionLike.builder().user(me).opinion(opinion1).build());
        entityManager.persistAndFlush(OpinionLike.builder().user(me).opinion(opinion2).build());
        entityManager.persistAndFlush(OpinionLike.builder().user(me).opinion(deletedOpinion).build());
        deletedOpinion.delete();
        entityManager.persistAndFlush(deletedOpinion);

        Page<LikedOpinionProjection> results = opinionQueryRepository.findLikedOpinions(me.getId(), PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(LikedOpinionProjection::opinionId)
                .containsExactly(opinion2.getId(), opinion1.getId());
        assertThat(results.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("좋아요가 없으면 빈 목록을 반환한다")
    void findLikedOpinionsReturnsEmptyWhenNoLikes() {
        Page<LikedOpinionProjection> results = opinionQueryRepository.findLikedOpinions(me.getId(), PageRequest.of(0, 20));

        assertThat(results.getContent()).isEmpty();
        assertThat(results.getTotalElements()).isZero();
    }
}
