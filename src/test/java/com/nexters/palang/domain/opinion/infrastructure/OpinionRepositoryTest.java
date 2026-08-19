package com.nexters.palang.domain.opinion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
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
class OpinionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OpinionRepository opinionRepository;

    private Book book;
    private Passage passage;

    private User user(String nickname) {
        return entityManager.persistAndFlush(User.builder()
                .nickname(nickname)
                .snsProvider(SnsProvider.KAKAO)
                .snsId(nickname)
                .termsAgreedAt(LocalDateTime.now())
                .build());
    }

    @BeforeEach
    void setUp() {
        User creator = user("작성자");
        book = entityManager.persistAndFlush(Book.builder()
                .title("책").author("작가").publisher("출판사").pageCount(300).build());
        passage = entityManager.persistAndFlush(Passage.builder()
                .book(book).creator(creator).pageNumber(1).quotedText("발췌 문장").isSpoiler(false)
                .normalizedHash("hash").build());
    }

    @Test
    @DisplayName("책에 살아있는 의견 수를 센다 (삭제된 의견은 제외)")
    void countByPassageBookIdAndDeletedAtIsNull() {
        User writer = user("작성자2");
        Opinion alive = entityManager.persistAndFlush(
                Opinion.builder().passage(passage).user(writer).content("살아있음").build());
        Opinion deleted = entityManager.persistAndFlush(
                Opinion.builder().passage(passage).user(writer).content("삭제됨").build());
        deleted.delete();
        entityManager.persistAndFlush(deleted);

        long count = opinionRepository.countByPassage_Book_IdAndDeletedAtIsNull(book.getId());

        assertThat(count).isEqualTo(1);
        assertThat(alive.getId()).isNotNull();
    }

    @Test
    @DisplayName("책에 의견을 남긴 사용자 id 목록을 중복 없이 조회한다")
    void findDistinctUserIdsByBookId() {
        User writerA = user("작성자A");
        User writerB = user("작성자B");
        entityManager.persistAndFlush(Opinion.builder().passage(passage).user(writerA).content("의견1").build());
        entityManager.persistAndFlush(Opinion.builder().passage(passage).user(writerA).content("의견2").build());
        entityManager.persistAndFlush(Opinion.builder().passage(passage).user(writerB).content("의견3").build());

        List<Long> userIds = opinionRepository.findDistinctUserIdsByBookId(book.getId());

        assertThat(userIds).containsExactlyInAnyOrder(writerA.getId(), writerB.getId());
    }
}
