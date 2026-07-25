package com.nexters.palang.domain.comment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class CommentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommentRepository commentRepository;

    private Opinion opinion;
    private User writer;

    @BeforeEach
    void setUp() {
        writer = entityManager.persistAndFlush(User.builder()
                .nickname("작성자")
                .snsProvider(SnsProvider.KAKAO)
                .snsId("writer")
                .termsAgreedAt(LocalDateTime.now())
                .build());
        Book book = entityManager.persistAndFlush(Book.builder()
                .title("책").author("작가").publisher("출판사").pageCount(300).build());
        Passage passage = entityManager.persistAndFlush(Passage.builder()
                .book(book).creator(writer).pageNumber(1).quotedText("발췌 문장").isSpoiler(false)
                .normalizedHash("hash").build());
        opinion = entityManager.persistAndFlush(
                Opinion.builder().passage(passage).user(writer).content("흔적 내용").build());
    }

    @Test
    @DisplayName("원댓글을 저장하면 부모 댓글 없이 조회된다")
    void saveRootComment() {
        Comment saved = commentRepository.save(Comment.root(opinion, writer, "원댓글"));

        Optional<Comment> found = commentRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getParentComment()).isNull();
        assertThat(found.get().getContent()).isEqualTo("원댓글");
    }

    @Test
    @DisplayName("답글을 저장하면 부모 댓글과 함께 조회된다")
    void saveReplyComment() {
        Comment root = commentRepository.save(Comment.root(opinion, writer, "원댓글"));
        Comment reply = commentRepository.save(Comment.reply(root, writer, "답글"));

        Optional<Comment> found = commentRepository.findById(reply.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getParentComment().getId()).isEqualTo(root.getId());
    }
}
