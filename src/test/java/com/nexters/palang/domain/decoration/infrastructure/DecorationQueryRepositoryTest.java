package com.nexters.palang.domain.decoration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.decoration.application.DecorationMergeCandidate;
import com.nexters.palang.domain.decoration.domain.Decoration;
import com.nexters.palang.domain.decoration.domain.EffectType;
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
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class DecorationQueryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    private DecorationQueryRepository decorationQueryRepository;

    @BeforeEach
    void setUp() {
        decorationQueryRepository = new DecorationQueryRepository(new JPAQueryFactory(entityManager.getEntityManager()));
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

    private Opinion opinion(Passage passage, User writer, String content) {
        return entityManager.persistAndFlush(Opinion.builder().passage(passage).user(writer).content(content).build());
    }

    private Decoration decoration(Opinion opinion, int start, int end) {
        return entityManager.persistAndFlush(Decoration.builder()
                .opinion(opinion).startOffset(start).endOffset(end).effectType(EffectType.UNDERLINE).build());
    }

    @Test
    @DisplayName("삭제되지 않은 흔적의 꾸밈만 후보로 조회하고 좋아요 수는 흔적의 값을 그대로 담는다")
    void findMergeCandidatesExcludesDeletedOpinions() {
        User writer = user("writer-1");
        Passage passage = passage(writer);
        Opinion activeOpinion = opinion(passage, writer, "활성 흔적");
        setLikeCount(activeOpinion, 7);
        Decoration activeDecoration = decoration(activeOpinion, 0, 5);

        Opinion deletedOpinion = opinion(passage, writer, "삭제된 흔적");
        deletedOpinion.delete();
        entityManager.persistAndFlush(deletedOpinion);
        decoration(deletedOpinion, 5, 10);

        List<DecorationMergeCandidate> candidates = decorationQueryRepository.findMergeCandidates(passage.getId());

        assertThat(candidates).extracting(DecorationMergeCandidate::decorationId)
                .containsExactly(activeDecoration.getId());
        assertThat(candidates.get(0).likeCount()).isEqualTo(7);
    }

    private void setLikeCount(Opinion opinion, int likeCount) {
        ReflectionTestUtils.setField(opinion, "likeCount", likeCount);
        entityManager.persistAndFlush(opinion);
    }
}
