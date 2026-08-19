package com.nexters.palang.domain.passage.infrastructure;

import com.nexters.palang.domain.book.application.BookOptionProjection;
import com.nexters.palang.domain.book.domain.QBook;
import com.nexters.palang.domain.opinion.domain.QOpinion;
import com.nexters.palang.domain.passage.application.MyPassageProjection;
import com.nexters.palang.domain.passage.application.SimilarPassageProjection;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.domain.QPassage;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PassageQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 같은 책의 인접 페이지(±1) 안에서 정규화 해시가 같은 Passage 후보를 연결된 흔적 수와 함께 조회한다. (FR-WRITE-07)
    // groupId가 null이면 전역 공개 대목만, 값이 있으면 그 모임 전용 대목만 후보로 삼는다(서로 섞이지 않는다).
    public List<SimilarPassageProjection> findSimilarCandidates(Long bookId, int pageNumber, String normalizedHash, Long groupId) {
        QPassage passage = QPassage.passage;
        QOpinion opinion = QOpinion.opinion;

        return queryFactory
                .select(Projections.constructor(SimilarPassageProjection.class,
                        passage.id, passage.quotedText, passage.pageNumber, opinion.countDistinct()))
                .from(passage)
                .leftJoin(opinion).on(opinion.passage.eq(passage).and(opinion.deletedAt.isNull()))
                .where(
                        passage.book.id.eq(bookId),
                        passage.deletedAt.isNull(),
                        passage.pageNumber.between(pageNumber - 1, pageNumber + 1),
                        passage.normalizedHash.eq(normalizedHash),
                        groupFilter(passage, groupId)
                )
                .groupBy(passage.id, passage.quotedText, passage.pageNumber)
                .orderBy(passage.pageNumber.asc(), passage.id.asc())
                .fetch();
    }

    // 대목/흔적 조회용 페이지 번호 목록: 대목이 걸쳐 있는 서로 다른 페이지 번호를 오름차순으로.
    public Page<Integer> findPageNumbers(Long bookId, Long groupId, Pageable pageable) {
        QPassage passage = QPassage.passage;
        BooleanExpression groupFilter = groupFilter(passage, groupId);

        List<Integer> content = queryFactory
                .select(passage.pageNumber)
                .from(passage)
                .where(passage.book.id.eq(bookId), passage.deletedAt.isNull(), groupFilter)
                .groupBy(passage.pageNumber)
                .orderBy(passage.pageNumber.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(passage.pageNumber.countDistinct())
                .from(passage)
                .where(passage.book.id.eq(bookId), passage.deletedAt.isNull(), groupFilter)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // 대목 전환용: 특정 페이지에 걸친 대목들을 등록 순으로.
    public List<Passage> findPassagesByPage(Long bookId, Long groupId, int pageNumber) {
        QPassage passage = QPassage.passage;

        return queryFactory
                .selectFrom(passage)
                .where(passage.book.id.eq(bookId), passage.pageNumber.eq(pageNumber), passage.deletedAt.isNull(),
                        groupFilter(passage, groupId))
                .orderBy(passage.id.asc())
                .fetch();
    }

    // groupId가 없으면(null) 전역 공개 대목(group_id IS NULL)만, 있으면 그 모임 소속 대목만 대상으로 한다.
    private BooleanExpression groupFilter(QPassage passage, Long groupId) {
        return groupId != null ? passage.group.id.eq(groupId) : passage.group.isNull();
    }

    // 내가 남긴 대목(FR-...): 소유 기준은 흔적을 남긴 사용자다(최초 생성자가 아니어도 병합된 대목에 흔적을 남겼으면 포함).
    // createdAt/정렬 기준은 대목 자체가 아니라 "내가 그 대목에 흔적을 남긴 시점"이어야 사용자 관점에서 의미가 있으므로
    // opinion.createdAt의 최댓값을 쓴다(같은 대목에 흔적을 여러 번 남긴 경우 가장 최근 흔적 기준, 대목 단위 중복 제거).
    public Page<MyPassageProjection> findMyPassages(Long userId, Long bookId, boolean spoilerOnly, Pageable pageable) {
        QOpinion opinion = QOpinion.opinion;
        QPassage passage = QPassage.passage;

        BooleanExpression bookFilter = bookId != null ? passage.book.id.eq(bookId) : null;
        BooleanExpression spoilerFilter = spoilerOnly ? passage.isSpoiler.isTrue() : null;

        List<MyPassageProjection> content = queryFactory
                .select(Projections.constructor(MyPassageProjection.class,
                        passage.id, passage.book.id, passage.pageNumber, passage.quotedText,
                        passage.isSpoiler, opinion.createdAt.max()))
                .from(opinion)
                .join(opinion.passage, passage)
                .where(opinion.user.id.eq(userId), opinion.deletedAt.isNull(), passage.deletedAt.isNull(),
                        bookFilter, spoilerFilter)
                .groupBy(passage.id, passage.book.id, passage.pageNumber, passage.quotedText, passage.isSpoiler)
                .orderBy(opinion.createdAt.max().desc(), passage.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(passage.countDistinct())
                .from(opinion)
                .join(opinion.passage, passage)
                .where(opinion.user.id.eq(userId), opinion.deletedAt.isNull(), passage.deletedAt.isNull(),
                        bookFilter, spoilerFilter)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // 좋아요·스포일러 관리 화면 공용 "전체 책 보기" 드롭다운: 내가 스포일러로 남긴 대목이 있는 도서 목록.
    // 소유 기준은 findMyPassages와 동일하게 흔적을 남긴 사용자(opinion.user) 기준.
    public List<BookOptionProjection> findSpoilerBookOptions(Long userId) {
        QOpinion opinion = QOpinion.opinion;
        QPassage passage = QPassage.passage;
        QBook book = QBook.book;

        return queryFactory
                .select(Projections.constructor(BookOptionProjection.class, book.id, book.title))
                .from(opinion)
                .join(opinion.passage, passage)
                .join(passage.book, book)
                .where(opinion.user.id.eq(userId), opinion.deletedAt.isNull(), passage.deletedAt.isNull(),
                        passage.isSpoiler.isTrue())
                .groupBy(book.id, book.title)
                .orderBy(opinion.createdAt.max().desc())
                .fetch();
    }
}
