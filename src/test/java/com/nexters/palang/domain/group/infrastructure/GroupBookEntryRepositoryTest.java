package com.nexters.palang.domain.group.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.GroupStatus;
import com.nexters.palang.domain.group.domain.ReadingPeriod;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class GroupBookEntryRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GroupBookEntryRepository groupBookEntryRepository;

    private User user(String snsId) {
        return entityManager.persistAndFlush(User.builder()
                .nickname("닉네임" + snsId)
                .snsProvider(SnsProvider.KAKAO)
                .snsId(snsId)
                .build());
    }

    private Book book(String title) {
        return entityManager.persistAndFlush(Book.builder()
                .title(title).author("한강").publisher("창비").pageCount(268).source(BookSource.API).build());
    }

    private Group group(User host) {
        return entityManager.persistAndFlush(Group.create("모임", host, 4));
    }

    private GroupBookEntry entry(Group group, Book book, LocalDate startDate, LocalDate endDate) {
        return entityManager.persistAndFlush(
                GroupBookEntry.create(group, book, ReadingPeriod.of(startDate, endDate), TODAY));
    }

    @Test
    @DisplayName("종료 임박 알림 대상은 종료일이 그 날짜인 ACTIVE 모임의 책뿐이다")
    void findAllInActiveGroupsByEndDate() {
        User host = user("host");
        Book book = book("채식주의자");
        LocalDate tomorrow = TODAY.plusDays(1);
        GroupBookEntry endingTomorrow = entry(group(host), book, TODAY, tomorrow);
        entry(group(host), book, TODAY, tomorrow.plusDays(5));
        Group mergedGroup = group(host);
        entry(mergedGroup, book, TODAY, tomorrow);
        ReflectionTestUtils.setField(mergedGroup, "status", GroupStatus.MERGED);
        entityManager.persistAndFlush(mergedGroup);
        entityManager.clear();

        List<GroupBookEntry> results = groupBookEntryRepository.findAllInActiveGroupsByEndDate(tomorrow);

        assertThat(results).extracting(GroupBookEntry::getId).containsExactly(endingTomorrow.getId());
    }

    @Test
    @DisplayName("모임들의 책은 먼저 등록된 순서(id 오름차순)로 book과 함께 조회된다")
    void findAllWithBookByGroupIdInOrdersByRegistration() {
        User host = user("host");
        Group group = group(host);
        GroupBookEntry first = entry(group, book("첫 책"), null, null);
        GroupBookEntry second = entry(group, book("둘째 책"), null, null);
        entityManager.clear();

        List<GroupBookEntry> results = groupBookEntryRepository.findAllWithBookByGroupIdIn(List.of(group.getId()));

        assertThat(results).extracting(GroupBookEntry::getId).containsExactly(first.getId(), second.getId());
        assertThat(results.getFirst().getBook().getTitle()).isEqualTo("첫 책");
    }

    @Test
    @DisplayName("모임이 함께 읽는 책인지 판본(bookId)으로 확인한다")
    void existsByGroupIdAndBookId() {
        Group group = group(user("host"));
        Book readingBook = book("채식주의자");
        Book otherBook = book("소년이 온다");
        entry(group, readingBook, null, null);

        assertThat(groupBookEntryRepository.existsByGroupIdAndBookId(group.getId(), readingBook.getId())).isTrue();
        assertThat(groupBookEntryRepository.existsByGroupIdAndBookId(group.getId(), otherBook.getId())).isFalse();
    }

    @Test
    @DisplayName("관리자 도서 삭제: 이 책을 함께 읽는 모임 id를 중복 없이 조회한다")
    void findGroupIdsByBookId() {
        User host = user("host");
        Book book = book("채식주의자");
        Group groupA = group(host);
        Group groupB = group(host);
        entry(groupA, book, null, null);
        entry(groupA, book, null, null);
        entry(groupB, book, null, null);
        entry(group(host), book("다른 책"), null, null);

        assertThat(groupBookEntryRepository.findGroupIdsByBookId(book.getId()))
                .containsExactlyInAnyOrder(groupA.getId(), groupB.getId());
    }
}
