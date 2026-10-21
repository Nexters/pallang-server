package com.nexters.palang.domain.opinion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.admin.application.AdminSortOrder;
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

    // 같은 트랜잭션 안에서 연달아 저장하면 createdAt이 같아질 수 있어, 정렬 검증용으로 시각을 직접 박아 둔다.
    private void setCreatedAt(Opinion opinion, LocalDateTime createdAt) {
        entityManager.getEntityManager()
                .createQuery("update Opinion o set o.createdAt = :createdAt where o.id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", opinion.getId())
                .executeUpdate();
        entityManager.clear();
    }

    @Test
    @DisplayName("관리자 의견 검색: 정렬을 고르면 작성일 최신순/오래된순으로 나온다")
    void findByContentContainingHonorsAdminSortOrder() {
        User writer = user("작성자");
        Passage passage = passage(writer);
        Opinion old = entityManager.persistAndFlush(Opinion.builder().passage(passage).user(writer).content("옛 의견").build());
        Opinion recent = entityManager.persistAndFlush(Opinion.builder().passage(passage).user(writer).content("새 의견").build());
        setCreatedAt(old, LocalDateTime.of(2026, 1, 1, 9, 0));
        setCreatedAt(recent, LocalDateTime.of(2026, 10, 1, 9, 0));

        Page<Opinion> latest = opinionRepository.findByContentContaining(
                "의견", PageRequest.of(0, 20, AdminSortOrder.LATEST.toSort()));
        Page<Opinion> oldest = opinionRepository.findByContentContaining(
                "의견", PageRequest.of(0, 20, AdminSortOrder.OLDEST.toSort()));

        assertThat(latest.getContent()).extracting(Opinion::getId).containsExactly(recent.getId(), old.getId());
        assertThat(oldest.getContent()).extracting(Opinion::getId).containsExactly(old.getId(), recent.getId());
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
