package com.nexters.palang.domain.group.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.block.domain.UserBlock;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.ReadingPeriod;
import com.nexters.palang.domain.group.domain.ReadingStatus;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.config.JpaAuditingConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
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
class GroupBookQueryRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    @Autowired
    private TestEntityManager entityManager;

    private GroupBookQueryRepository repository;

    private User host;
    private Group group;

    @BeforeEach
    void setUp() {
        repository = new GroupBookQueryRepository(new JPAQueryFactory(entityManager.getEntityManager()));
        host = user("host");
        group = entityManager.persistAndFlush(Group.create("고전 뽀개기", host, 4));
    }

    private User user(String snsId) {
        return entityManager.persistAndFlush(User.builder()
                .nickname("닉네임" + snsId).snsProvider(SnsProvider.KAKAO).snsId(snsId).build());
    }

    private Book book(String title) {
        return entityManager.persistAndFlush(Book.builder()
                .title(title).author("한강").publisher("창비").pageCount(268).source(BookSource.API).build());
    }

    // 이미 저장돼 있던 과거 기간도 만들 수 있게 검증 기준일을 아주 이른 날짜로 둔다.
    private GroupBookEntry entry(Group group, String title, ReadingPeriod period) {
        return entityManager.persistAndFlush(GroupBookEntry.create(group, book(title), period, LocalDate.MIN));
    }

    private void registeredAt(GroupBookEntry entry, LocalDateTime registeredAt) {
        entityManager.getEntityManager()
                .createNativeQuery("update group_book_entries set created_at = :at where id = :id")
                .setParameter("at", registeredAt).setParameter("id", entry.getId())
                .executeUpdate();
    }

    private Passage passage(Book book, Group group, User creator) {
        return entityManager.persistAndFlush(Passage.builder()
                .book(book).creator(creator).group(group).pageNumber(1).quotedText("발췌 문장").isSpoiler(false)
                .normalizedHash("hash").build());
    }

    private Opinion opinion(Passage passage, User writer) {
        return entityManager.persistAndFlush(Opinion.builder().passage(passage).user(writer).content("의견").build());
    }

    @Test
    @DisplayName("날짜 자동 판정과 직접 고른 상태를 함께 반영해 읽는 중/완독한 책을 나눈다")
    void filtersByComputedStatus() {
        GroupBookEntry noPeriod = entry(group, "기간 없음", ReadingPeriod.NONE);
        GroupBookEntry endsToday = entry(group, "오늘 끝남", ReadingPeriod.of(TODAY.minusDays(5), TODAY));
        GroupBookEntry endedYesterday = entry(group, "어제 끝남", ReadingPeriod.of(TODAY.minusDays(5), TODAY.minusDays(1)));
        GroupBookEntry manuallyFinished = entry(group, "직접 완독", ReadingPeriod.of(TODAY, TODAY.plusDays(10)));
        manuallyFinished.changeStatusManually(ReadingStatus.FINISHED);
        GroupBookEntry manuallyReading = entry(group, "직접 읽는 중", ReadingPeriod.of(TODAY.minusDays(9), TODAY.minusDays(3)));
        manuallyReading.changeStatusManually(ReadingStatus.READING);
        entityManager.flush();
        entityManager.clear();

        List<Long> reading = repository.findBooks(group.getId(), ReadingStatus.READING, TODAY, PageRequest.of(0, 20))
                .map(GroupBookEntry::getId).getContent();
        List<Long> finished = repository.findBooks(group.getId(), ReadingStatus.FINISHED, TODAY, PageRequest.of(0, 20))
                .map(GroupBookEntry::getId).getContent();

        assertThat(reading).containsExactlyInAnyOrder(noPeriod.getId(), endsToday.getId(), manuallyReading.getId());
        assertThat(finished).containsExactlyInAnyOrder(endedYesterday.getId(), manuallyFinished.getId());
        assertThat(repository.countBooks(group.getId(), ReadingStatus.READING, TODAY)).isEqualTo(3);
        assertThat(repository.countBooks(group.getId(), ReadingStatus.FINISHED, TODAY)).isEqualTo(2);
        assertThat(repository.countBooks(group.getId(), null, TODAY)).isEqualTo(5);
    }

    @Test
    @DisplayName("쿼리의 상태 판정은 엔티티의 currentStatus와 항상 같다")
    void queryStatusMatchesEntityStatus() {
        entry(group, "기간 없음", ReadingPeriod.NONE);
        entry(group, "오늘 끝남", ReadingPeriod.of(TODAY.minusDays(5), TODAY));
        entry(group, "어제 끝남", ReadingPeriod.of(TODAY.minusDays(5), TODAY.minusDays(1)));
        entityManager.clear();

        Page<GroupBookEntry> reading = repository.findBooks(group.getId(), ReadingStatus.READING, TODAY, PageRequest.of(0, 20));
        Page<GroupBookEntry> finished = repository.findBooks(group.getId(), ReadingStatus.FINISHED, TODAY, PageRequest.of(0, 20));

        assertThat(reading.getContent()).allMatch(entry -> entry.currentStatus(TODAY) == ReadingStatus.READING);
        assertThat(finished.getContent()).allMatch(entry -> entry.currentStatus(TODAY) == ReadingStatus.FINISHED);
    }

    @Test
    @DisplayName("책 목록은 등록 시각 내림차순(새 책이 맨 위), 같으면 entryId 오름차순이다")
    void ordersByRegisteredAtDescThenIdAsc() {
        GroupBookEntry oldest = entry(group, "가장 먼저", ReadingPeriod.NONE);
        GroupBookEntry tieA = entry(group, "같은 시각 A", ReadingPeriod.NONE);
        GroupBookEntry tieB = entry(group, "같은 시각 B", ReadingPeriod.NONE);
        registeredAt(oldest, LocalDateTime.of(2026, 11, 1, 9, 0));
        registeredAt(tieA, LocalDateTime.of(2026, 11, 10, 9, 0));
        registeredAt(tieB, LocalDateTime.of(2026, 11, 10, 9, 0));
        entityManager.clear();

        List<Long> ids = repository.findBooks(group.getId(), null, TODAY, PageRequest.of(0, 20))
                .map(GroupBookEntry::getId).getContent();

        assertThat(ids).containsExactly(tieA.getId(), tieB.getId(), oldest.getId());
    }

    @Test
    @DisplayName("다른 모임의 책은 목록에 섞이지 않는다")
    void excludesOtherGroupsBooks() {
        GroupBookEntry mine = entry(group, "내 모임 책", ReadingPeriod.NONE);
        entry(entityManager.persistAndFlush(Group.create("다른 모임", host, 4)), "남의 책", ReadingPeriod.NONE);

        assertThat(repository.findBooks(group.getId(), null, TODAY, PageRequest.of(0, 20)).getContent())
                .extracting(GroupBookEntry::getId).containsExactly(mine.getId());
    }

    @Test
    @DisplayName("기록 수는 이 모임의 지워지지 않은 문장과 의견만 세고, 내가 차단한 작성자의 의견은 뺀다")
    void countsPassagesAndOpinionsOfThisGroupOnly() {
        User me = user("me");
        User blockedWriter = user("blocked");
        entityManager.persistAndFlush(UserBlock.of(me, blockedWriter));
        GroupBookEntry entry = entry(group, "채식주의자", ReadingPeriod.NONE);
        Book book = entry.getBook();

        Passage passage = passage(book, group, host);
        opinion(passage, host);
        opinion(passage, me);
        opinion(passage, blockedWriter);
        Opinion deletedOpinion = opinion(passage, host);
        deletedOpinion.delete();
        Passage deletedPassage = passage(book, group, host);
        opinion(deletedPassage, host);
        deletedPassage.delete();
        Group otherGroup = entityManager.persistAndFlush(Group.create("다른 모임", host, 4));
        opinion(passage(book, otherGroup, host), host);
        opinion(passage(book, null, host), host);
        entityManager.flush();

        Map<Long, Long> passageCounts = repository.countPassagesByBook(group.getId(), List.of(book.getId()));
        Map<Long, Long> opinionCounts = repository.countOpinionsByBook(group.getId(), List.of(book.getId()), me.getId());
        Map<Long, Long> opinionCountsForGuest = repository.countOpinionsByBook(group.getId(), List.of(book.getId()), null);

        assertThat(passageCounts).containsEntry(book.getId(), 1L);
        assertThat(opinionCounts).containsEntry(book.getId(), 2L);
        assertThat(opinionCountsForGuest).containsEntry(book.getId(), 3L);
    }
}
