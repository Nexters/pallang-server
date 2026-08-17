package com.nexters.palang.domain.passage.infrastructure;

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
    public List<SimilarPassageProjection> findSimilarCandidates(Long bookId, int pageNumber, String normalizedHash) {
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
                        passage.normalizedHash.eq(normalizedHash)
                )
                .groupBy(passage.id, passage.quotedText, passage.pageNumber)
                .orderBy(passage.pageNumber.asc(), passage.id.asc())
                .fetch();
    }

    // 대목/흔적 조회용 페이지 번호 목록: 대목이 걸쳐 있는 서로 다른 페이지 번호를 오름차순으로.
    public Page<Integer> findPageNumbers(Long bookId, Pageable pageable) {
        QPassage passage = QPassage.passage;

        List<Integer> content = queryFactory
                .select(passage.pageNumber)
                .from(passage)
                .where(passage.book.id.eq(bookId), passage.deletedAt.isNull())
                .groupBy(passage.pageNumber)
                .orderBy(passage.pageNumber.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(passage.pageNumber.countDistinct())
                .from(passage)
                .where(passage.book.id.eq(bookId), passage.deletedAt.isNull())
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // 대목 전환용: 특정 페이지에 걸친 대목들을 등록 순으로.
    public List<Passage> findPassagesByPage(Long bookId, int pageNumber) {
        QPassage passage = QPassage.passage;

        return queryFactory
                .selectFrom(passage)
                .where(passage.book.id.eq(bookId), passage.pageNumber.eq(pageNumber), passage.deletedAt.isNull())
                .orderBy(passage.id.asc())
                .fetch();
    }

    // 내가 남긴 대목(FR-...): 소유 기준은 흔적을 남긴 사용자다(최초 생성자가 아니어도 병합된 대목에 흔적을 남겼으면 포함).
    // 같은 대목에 흔적을 여러 번 남긴 경우 중복 제거를 위해 대목 단위로 group by 한다.
    public Page<MyPassageProjection> findMyPassages(Long userId, Long bookId, boolean spoilerOnly, Pageable pageable) {
        QOpinion opinion = QOpinion.opinion;
        QPassage passage = QPassage.passage;

        BooleanExpression bookFilter = bookId != null ? passage.book.id.eq(bookId) : null;
        BooleanExpression spoilerFilter = spoilerOnly ? passage.isSpoiler.isTrue() : null;

        List<MyPassageProjection> content = queryFactory
                .select(Projections.constructor(MyPassageProjection.class,
                        passage.id, passage.book.id, passage.pageNumber, passage.quotedText,
                        passage.isSpoiler, passage.createdAt))
                .from(opinion)
                .join(opinion.passage, passage)
                .where(opinion.user.id.eq(userId), opinion.deletedAt.isNull(), passage.deletedAt.isNull(),
                        bookFilter, spoilerFilter)
                .groupBy(passage.id, passage.book.id, passage.pageNumber, passage.quotedText,
                        passage.isSpoiler, passage.createdAt)
                .orderBy(passage.createdAt.desc(), passage.id.desc())
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
}
