package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.block.domain.QUserBlock;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.QGroupBookEntry;
import com.nexters.palang.domain.group.domain.ReadingStatus;
import com.nexters.palang.domain.group.domain.ReadingStatusMode;
import com.nexters.palang.domain.opinion.domain.QOpinion;
import com.nexters.palang.domain.passage.domain.QPassage;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GroupBookQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 모임 책 목록. status가 null이면 전체. 등록 시각 내림차순(새 책이 맨 위), 같으면 entryId 오름차순(L09).
    // 상태는 저장값이 아니라 today 기준으로 계산한 값으로 거른다(GroupBookEntry.currentStatus와 같은 규칙).
    public Page<GroupBookEntry> findBooks(Long groupId, ReadingStatus status, LocalDate today, Pageable pageable) {
        QGroupBookEntry entry = QGroupBookEntry.groupBookEntry;
        BooleanExpression condition = entry.group.id.eq(groupId).and(statusIs(entry, status, today));

        List<GroupBookEntry> content = queryFactory
                .selectFrom(entry)
                .join(entry.book).fetchJoin()
                .where(condition)
                .orderBy(entry.createdAt.desc(), entry.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory.select(entry.count()).from(entry).where(condition).fetchOne();
        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    public long countBooks(Long groupId, ReadingStatus status, LocalDate today) {
        QGroupBookEntry entry = QGroupBookEntry.groupBookEntry;
        Long count = queryFactory
                .select(entry.count())
                .from(entry)
                .where(entry.group.id.eq(groupId), statusIs(entry, status, today))
                .fetchOne();
        return count != null ? count : 0L;
    }

    // 모임의 각 책(bookId)에 쌓인 지워지지 않은 문장 수.
    public Map<Long, Long> countPassagesByBook(Long groupId, Collection<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        QPassage passage = QPassage.passage;
        return queryFactory
                .select(passage.book.id, passage.count())
                .from(passage)
                .where(passage.group.id.eq(groupId), passage.book.id.in(bookIds), passage.deletedAt.isNull())
                .groupBy(passage.book.id)
                .fetch().stream()
                .collect(Collectors.toMap(t -> t.get(passage.book.id), t -> t.get(passage.count())));
    }

    // 모임의 각 책(bookId)에 달린 지워지지 않은 의견 수. 지워진 문장의 의견과, 조회자가 차단한 작성자의 의견은 뺀다.
    // 댓글(답글)과 좋아요는 별도 테이블이라 자연히 세지 않는다.
    public Map<Long, Long> countOpinionsByBook(Long groupId, Collection<Long> bookIds, Long viewerId) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        QOpinion opinion = QOpinion.opinion;
        QPassage passage = QPassage.passage;
        QUserBlock userBlock = QUserBlock.userBlock;
        BooleanExpression notBlocked = viewerId != null
                ? opinion.user.id.notIn(JPAExpressions
                        .select(userBlock.blocked.id)
                        .from(userBlock)
                        .where(userBlock.blocker.id.eq(viewerId)))
                : null;
        return queryFactory
                .select(passage.book.id, opinion.count())
                .from(opinion)
                .join(opinion.passage, passage)
                .where(passage.group.id.eq(groupId), passage.book.id.in(bookIds), passage.deletedAt.isNull(),
                        opinion.deletedAt.isNull(), notBlocked)
                .groupBy(passage.book.id)
                .fetch().stream()
                .collect(Collectors.toMap(t -> t.get(passage.book.id), t -> t.get(opinion.count())));
    }

    // GroupBookEntry.currentStatus(today)를 쿼리 조건으로 옮긴 것. 둘이 어긋나면 탭 개수와 카드 상태가 달라지므로
    // 규칙을 바꿀 때는 함께 바꾼다.
    //   READING  = (AUTO and (종료일 없음 or 종료일 >= today)) or (MANUAL and 수동값 READING)
    //   FINISHED = (AUTO and 종료일 < today) or (MANUAL and 수동값 FINISHED)
    private BooleanExpression statusIs(QGroupBookEntry entry, ReadingStatus status, LocalDate today) {
        if (status == null) {
            return null;
        }
        BooleanExpression auto = entry.statusMode.eq(ReadingStatusMode.AUTO);
        BooleanExpression manual = entry.statusMode.eq(ReadingStatusMode.MANUAL).and(entry.manualStatus.eq(status));
        BooleanExpression autoMatches = status == ReadingStatus.READING
                ? auto.and(entry.endDate.isNull().or(entry.endDate.goe(today)))
                : auto.and(entry.endDate.lt(today));
        return autoMatches.or(manual);
    }
}
