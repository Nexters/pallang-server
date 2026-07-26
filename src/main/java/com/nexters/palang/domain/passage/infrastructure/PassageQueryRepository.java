package com.nexters.palang.domain.passage.infrastructure;

import com.nexters.palang.domain.opinion.domain.QOpinion;
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
                        passage.pageNumber.between(pageNumber - 1, pageNumber + 1),
                        passage.normalizedHash.eq(normalizedHash)
                )
                .groupBy(passage.id, passage.quotedText, passage.pageNumber)
                .orderBy(passage.pageNumber.asc(), passage.id.asc())
                .fetch();
    }

    // 읽기상태 노출 필터(FR-WRITE-08)의 "PLANNED/미설정/비로그인" 기준 페이지: 가장 작은 페이지 번호.
    // 스포일러 여부는 여기서 걸러내지 않는다 (내용 마스킹은 응답을 만드는 쪽의 책임, FR-VIEW-03).
    public Integer findFirstVisiblePageNumber(Long bookId) {
        QPassage passage = QPassage.passage;
        return queryFactory
                .select(passage.pageNumber.min())
                .from(passage)
                .where(passage.book.id.eq(bookId))
                .fetchOne();
    }

    // 대목/흔적 조회(FR-VIEW-02)용 페이지 번호 목록: 노출 필터를 만족하는 대목이 걸쳐 있는 서로 다른 페이지 번호를 오름차순으로.
    public Page<Integer> findVisiblePageNumbers(Long bookId, BooleanExpression visibilityFilter, Pageable pageable) {
        QPassage passage = QPassage.passage;

        List<Integer> content = queryFactory
                .select(passage.pageNumber)
                .from(passage)
                .where(passage.book.id.eq(bookId), visibilityFilter)
                .groupBy(passage.pageNumber)
                .orderBy(passage.pageNumber.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(passage.pageNumber.countDistinct())
                .from(passage)
                .where(passage.book.id.eq(bookId), visibilityFilter)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // 대목 전환(FR-VIEW-03 2-b)용: 특정 페이지에 걸친 대목들을 등록 순으로. 노출 필터를 만족하지 않으면 빈 리스트.
    public List<Passage> findVisiblePassagesByPage(Long bookId, int pageNumber, BooleanExpression visibilityFilter) {
        QPassage passage = QPassage.passage;

        return queryFactory
                .selectFrom(passage)
                .where(passage.book.id.eq(bookId), passage.pageNumber.eq(pageNumber), visibilityFilter)
                .orderBy(passage.id.asc())
                .fetch();
    }
}
