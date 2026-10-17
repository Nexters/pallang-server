package com.nexters.palang.domain.group.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class GroupRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GroupRepository groupRepository;

    private User user(String snsId) {
        return entityManager.persistAndFlush(User.builder()
                .nickname("닉네임" + snsId)
                .snsProvider(SnsProvider.KAKAO)
                .snsId(snsId)
                .build());
    }

    private Book book() {
        return entityManager.persistAndFlush(Book.builder()
                .title("채식주의자").author("한강").publisher("창비").pageCount(268).source(BookSource.API).build());
    }

    private Group group(Book book, User host, LocalDate startDate, LocalDate endDate) {
        Group built = Group.create("모임", book, host, 4, startDate, endDate);
        return entityManager.persistAndFlush(built);
    }

    @Test
    @DisplayName("종료 임박 알림(GroupEndingSoonNotifier): 종료일이 특정 날짜인 모임만 조회한다")
    void findAllByEndDate() {
        Book book = book();
        User host = user("host");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Group endingTomorrow = group(book, host, LocalDate.now(), tomorrow);
        group(book, host, LocalDate.now(), tomorrow.plusDays(5));

        var results = groupRepository.findAllByEndDate(tomorrow);

        assertThat(results).extracting(Group::getId).containsExactly(endingTomorrow.getId());
    }
}
