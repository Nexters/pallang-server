package com.nexters.palang.domain.opinion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.opinion.application.LikedOpinionProjection;
import com.nexters.palang.domain.opinion.application.MyOpinionProjection;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.domain.OpinionLike;
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

    private Passage passage(int pageNumber) {
        return entityManager.persistAndFlush(Passage.builder()
                .book(book).creator(me).pageNumber(pageNumber).quotedText("발췌 문장").isSpoiler(false)
                .normalizedHash("hash-" + pageNumber).build());
    }

    private Opinion opinion(User user, Passage passage, String content) {
        return entityManager.persistAndFlush(Opinion.builder().passage(passage).user(user).content(content).build());
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
