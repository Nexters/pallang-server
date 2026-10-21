package com.nexters.palang.domain.opinion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.time.LocalDateTime;
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
class OpinionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OpinionRepository opinionRepository;

    private User user(String nickname) {
        return entityManager.persistAndFlush(User.builder()
                .nickname(nickname)
                .snsProvider(SnsProvider.KAKAO)
                .snsId(nickname)
                .termsAgreedAt(LocalDateTime.now())
                .build());
    }

    private Passage passage(User creator) {
        Book book = entityManager.persistAndFlush(Book.builder()
                .title("책").author("작가").publisher("출판사").pageCount(300).build());
        return entityManager.persistAndFlush(Passage.builder()
                .book(book).creator(creator).pageNumber(1).quotedText("발췌 문장").isSpoiler(false)
                .normalizedHash("hash").build());
    }

    @Test
    @DisplayName("관리자 페이지: 대목 행을 펼치면 그 대목의 의견을 작성 순서대로 조회한다")
    void findByPassageIdOrderByCreatedAtAscIdAsc() {
        User writer = user("작성자");
        Passage passage = passage(writer);
        Opinion first = entityManager.persistAndFlush(
                Opinion.builder().passage(passage).user(writer).content("첫 번째 의견").build());
        Opinion second = entityManager.persistAndFlush(
                Opinion.builder().passage(passage).user(writer).content("두 번째 의견").build());
        Passage otherPassage = passage(writer);
        entityManager.persistAndFlush(
                Opinion.builder().passage(otherPassage).user(writer).content("다른 대목의 의견").build());

        Page<Opinion> results = opinionRepository.findByPassageIdOrderByCreatedAtAscIdAsc(
                passage.getId(), PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(Opinion::getId).containsExactly(first.getId(), second.getId());
    }
}
