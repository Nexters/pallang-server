package com.nexters.palang.domain.comment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.block.domain.UserBlock;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
class CommentQueryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    private CommentQueryRepository commentQueryRepository;

    private Opinion opinion;
    private User writer;

    @BeforeEach
    void setUp() {
        commentQueryRepository = new CommentQueryRepository(new JPAQueryFactory(entityManager.getEntityManager()));

        writer = user("writer");
        Book book = entityManager.persistAndFlush(Book.builder()
                .title("책")
                .author("작가")
                .publisher("출판사")
                .pageCount(300)
                .build());
        Passage passage = entityManager.persistAndFlush(Passage.builder()
                .book(book)
                .creator(writer)
                .pageNumber(1)
                .quotedText("발췌 문장")
                .isSpoiler(false)
                .normalizedHash("hash")
                .build());
        opinion = entityManager.persistAndFlush(Opinion.builder()
                .passage(passage)
                .user(writer)
                .content("흔적 내용")
                .build());
    }

    private User user(String snsId) {
        return entityManager.persistAndFlush(User.builder()
                .nickname("닉네임" + snsId)
                .snsProvider(SnsProvider.KAKAO)
                .snsId(snsId)
                .termsAgreedAt(LocalDateTime.now())
                .build());
    }

    private Comment root(String content) {
        return entityManager.persistAndFlush(Comment.root(opinion, writer, content));
    }

    private Comment reply(Comment parent, String content) {
        return entityManager.persistAndFlush(Comment.reply(parent, writer, content));
    }

    @Test
    @DisplayName("원댓글만 작성 순서대로 조회하고 답글은 포함하지 않는다")
    void findRootCommentsOnlyReturnsRootsInCreationOrder() {
        Comment root1 = root("첫 댓글");
        Comment root2 = root("둘째 댓글");
        reply(root1, "답글");

        Page<Comment> results = commentQueryRepository.findRootComments(opinion.getId(), PageRequest.of(0, 20), null);

        assertThat(results.getContent()).extracting(Comment::getId).containsExactly(root1.getId(), root2.getId());
        assertThat(results.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("다른 흔적의 댓글은 조회되지 않는다")
    void findRootCommentsExcludesOtherOpinions() {
        Book book = entityManager.persistAndFlush(Book.builder()
                .title("다른 책").author("작가").publisher("출판사").pageCount(100).build());
        Passage otherPassage = entityManager.persistAndFlush(Passage.builder()
                .book(book).creator(writer).pageNumber(1).quotedText("문장").isSpoiler(false)
                .normalizedHash("other-hash").build());
        Opinion otherOpinion = entityManager.persistAndFlush(
                Opinion.builder().passage(otherPassage).user(writer).content("다른 흔적").build());
        entityManager.persistAndFlush(Comment.root(otherOpinion, writer, "다른 흔적의 댓글"));
        Comment myRoot = root("내 흔적의 댓글");

        Page<Comment> results = commentQueryRepository.findRootComments(opinion.getId(), PageRequest.of(0, 20), null);

        assertThat(results.getContent()).extracting(Comment::getId).containsExactly(myRoot.getId());
    }

    @Test
    @DisplayName("차단한 작성자의 원댓글은 목록에서 제외되고, 차단하지 않은 사용자에게는 그대로 보인다")
    void findRootCommentsExcludesCommentsFromBlockedWriter() {
        User me = user("me");
        User blockedWriter = user("blocked-w");
        Comment myRoot = root("내 댓글");
        Comment blockedRoot = entityManager.persistAndFlush(Comment.root(opinion, blockedWriter, "차단한 사람의 댓글"));
        entityManager.persistAndFlush(UserBlock.of(me, blockedWriter));

        Page<Comment> resultForMe = commentQueryRepository.findRootComments(opinion.getId(), PageRequest.of(0, 20), me.getId());
        Page<Comment> resultForAnonymous = commentQueryRepository.findRootComments(opinion.getId(), PageRequest.of(0, 20), null);

        assertThat(resultForMe.getContent()).extracting(Comment::getId).containsExactly(myRoot.getId());
        assertThat(resultForAnonymous.getContent()).extracting(Comment::getId)
                .containsExactlyInAnyOrder(myRoot.getId(), blockedRoot.getId());
    }

    @Test
    @DisplayName("여러 원댓글의 답글 개수를 부모 ID별로 집계한다")
    void countRepliesByParentIdsCountsPerParent() {
        Comment root1 = root("첫 댓글");
        Comment root2 = root("둘째 댓글");
        reply(root1, "첫 댓글의 답글1");
        reply(root1, "첫 댓글의 답글2");
        reply(root2, "둘째 댓글의 답글");

        Map<Long, Long> counts = commentQueryRepository.countRepliesByParentIds(List.of(root1.getId(), root2.getId()), null);

        assertThat(counts).containsEntry(root1.getId(), 2L).containsEntry(root2.getId(), 1L);
    }

    @Test
    @DisplayName("부모 ID 목록이 비어 있으면 빈 개수 맵을 반환한다")
    void countRepliesByParentIdsReturnsEmptyForEmptyInput() {
        Map<Long, Long> counts = commentQueryRepository.countRepliesByParentIds(List.of(), null);

        assertThat(counts).isEmpty();
    }

    @Test
    @DisplayName("차단한 작성자의 답글은 개수 집계에서 제외된다")
    void countRepliesByParentIdsExcludesRepliesFromBlockedWriter() {
        User me = user("me");
        User blockedWriter = user("blocked-w");
        Comment root1 = root("첫 댓글");
        reply(root1, "내 답글");
        entityManager.persistAndFlush(Comment.reply(root1, blockedWriter, "차단한 사람의 답글"));
        entityManager.persistAndFlush(UserBlock.of(me, blockedWriter));

        Map<Long, Long> countsForMe = commentQueryRepository.countRepliesByParentIds(List.of(root1.getId()), me.getId());
        Map<Long, Long> countsForAnonymous = commentQueryRepository.countRepliesByParentIds(List.of(root1.getId()), null);

        assertThat(countsForMe).containsEntry(root1.getId(), 1L);
        assertThat(countsForAnonymous).containsEntry(root1.getId(), 2L);
    }

    @Test
    @DisplayName("답글 미리보기는 부모마다 지정한 개수만큼만 오래된 순으로 조회한다")
    void findReplyPreviewsByParentIdsLimitsToPreviewSizePerParent() {
        Comment root1 = root("첫 댓글");
        Comment root2 = root("둘째 댓글");
        Comment root1Reply1 = reply(root1, "첫 댓글의 답글1");
        Comment root1Reply2 = reply(root1, "첫 댓글의 답글2");
        for (int i = 3; i <= 6; i++) {
            reply(root1, "첫 댓글의 답글" + i);
        }
        Comment root2Reply = reply(root2, "둘째 댓글의 답글");

        Map<Long, List<Comment>> previews = commentQueryRepository.findReplyPreviewsByParentIds(
                List.of(root1.getId(), root2.getId()), 2, null);

        assertThat(previews.get(root1.getId())).extracting(Comment::getId)
                .containsExactly(root1Reply1.getId(), root1Reply2.getId());
        assertThat(previews.get(root2.getId())).extracting(Comment::getId).containsExactly(root2Reply.getId());
    }

    @Test
    @DisplayName("부모 ID 목록이 비어 있으면 빈 미리보기 맵을 반환한다")
    void findReplyPreviewsByParentIdsReturnsEmptyForEmptyInput() {
        Map<Long, List<Comment>> previews = commentQueryRepository.findReplyPreviewsByParentIds(List.of(), 5, null);

        assertThat(previews).isEmpty();
    }

    @Test
    @DisplayName("특정 원댓글의 답글을 페이지네이션으로 조회한다")
    void findRepliesPaginatesRepliesOfSingleParent() {
        Comment rootComment = root("원댓글");
        for (int i = 1; i <= 6; i++) {
            reply(rootComment, "답글 " + i);
        }

        Page<Comment> firstPage = commentQueryRepository.findReplies(rootComment.getId(), PageRequest.of(0, 5), null);

        assertThat(firstPage.getContent()).hasSize(5);
        assertThat(firstPage.getTotalElements()).isEqualTo(6);
        assertThat(firstPage.hasNext()).isTrue();
    }

    @Test
    @DisplayName("차단한 작성자의 답글은 답글 더보기 목록에서 제외된다")
    void findRepliesExcludesRepliesFromBlockedWriter() {
        User me = user("me");
        User blockedWriter = user("blocked-w");
        Comment rootComment = root("원댓글");
        Comment myReply = reply(rootComment, "내 답글");
        entityManager.persistAndFlush(Comment.reply(rootComment, blockedWriter, "차단한 사람의 답글"));
        entityManager.persistAndFlush(UserBlock.of(me, blockedWriter));

        Page<Comment> resultForMe = commentQueryRepository.findReplies(rootComment.getId(), PageRequest.of(0, 20), me.getId());

        assertThat(resultForMe.getContent()).extracting(Comment::getId).containsExactly(myReply.getId());
    }
}
