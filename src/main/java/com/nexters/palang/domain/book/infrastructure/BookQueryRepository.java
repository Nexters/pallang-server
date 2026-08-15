package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.application.BookActivityProjection;
import com.nexters.palang.domain.book.application.BookSearchProjection;
import com.nexters.palang.domain.book.application.BookSearchSort;
import com.nexters.palang.domain.book.domain.QBook;
import com.nexters.palang.domain.opinion.domain.QOpinion;
import com.nexters.palang.domain.passage.domain.QPassage;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BookQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 띄어쓰기 차이를 무시하고 검색할 수 있도록 제목/키워드 모두에서 공백을 제거한 뒤 비교한다.
    // 흔적(Opinion)이 하나도 없는 도서는 결과에서 제외하므로 leftJoin이 아닌 innerJoin을 사용한다.
    public Page<BookSearchProjection> searchByTitle(String keyword, BookSearchSort sort, Pageable pageable) {
        QBook book = QBook.book;
        QPassage passage = QPassage.passage;
        QOpinion opinion = QOpinion.opinion;

        String normalizedKeyword = keyword.replace(" ", "");
        StringExpression normalizedTitle = Expressions.stringTemplate("replace({0}, ' ', '')", book.title);

        List<BookSearchProjection> content = queryFactory
                .select(Projections.constructor(BookSearchProjection.class,
                        book.id, book.title, book.author, book.publisher, book.pageCount,
                        book.isbn, book.coverImageUrl, book.source,
                        passage.countDistinct(), opinion.countDistinct()))
                .from(book)
                .innerJoin(passage).on(passage.book.eq(book))
                .innerJoin(opinion).on(opinion.passage.eq(passage).and(opinion.deletedAt.isNull()))
                .where(normalizedTitle.containsIgnoreCase(normalizedKeyword))
                .groupBy(book.id, book.title, book.author, book.publisher, book.pageCount,
                        book.isbn, book.coverImageUrl, book.source)
                .orderBy(searchOrderSpecifiers(sort, book, opinion))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(book.countDistinct())
                .from(book)
                .innerJoin(passage).on(passage.book.eq(book))
                .innerJoin(opinion).on(opinion.passage.eq(passage).and(opinion.deletedAt.isNull()))
                .where(normalizedTitle.containsIgnoreCase(normalizedKeyword))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private OrderSpecifier<?>[] searchOrderSpecifiers(BookSearchSort sort, QBook book, QOpinion opinion) {
        return switch (sort) {
            case NAME -> new OrderSpecifier[]{book.title.asc(), book.id.asc()};
            case RECENT -> new OrderSpecifier[]{opinion.createdAt.max().desc(), book.id.asc()};
            case OPINION -> new OrderSpecifier[]{opinion.countDistinct().desc(), book.id.asc()};
        };
    }

    // 홈 캐러셀은 전체 목록 중 임의의 가운데 offset에서 좌우로 조회해야 해서, page*size로 offset이
    // 고정되는 Pageable 대신 offset/limit을 직접 받는다.
    public List<BookActivityProjection> findCarouselBooks(long offset, int limit) {
        QBook book = QBook.book;
        QPassage passage = QPassage.passage;
        QOpinion opinion = QOpinion.opinion;

        return queryFactory
                .select(Projections.constructor(BookActivityProjection.class,
                        book.id, book.title, book.author, book.coverImageUrl,
                        passage.countDistinct(), opinion.countDistinct()))
                .from(book)
                .innerJoin(passage).on(passage.book.eq(book))
                .leftJoin(opinion).on(opinion.passage.eq(passage).and(opinion.deletedAt.isNull()))
                .groupBy(book.id, book.title, book.author, book.coverImageUrl)
                .orderBy(passage.createdAt.max().desc(), book.id.asc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public long countCarouselBooks() {
        return countBooksWithPassages();
    }

    // 내 서재는 로그인한 사용자가 흔적(Opinion)을 남긴 도서만 대상으로 하되, 대목/흔적 수는 홈 캐러셀과 동일하게
    // 도서 전체 기준으로 보여준다. 가운데 정렬 기준은 사용자가 해당 도서에 남긴 가장 최근 흔적 시각이다.
    public List<BookActivityProjection> findMyLibraryBooks(Long userId, long offset, int limit) {
        QBook book = QBook.book;
        QPassage passage = QPassage.passage;
        QOpinion opinionMine = new QOpinion("opinionMine");
        QOpinion opinionAll = new QOpinion("opinionAll");

        return queryFactory
                .select(Projections.constructor(BookActivityProjection.class,
                        book.id, book.title, book.author, book.coverImageUrl,
                        passage.countDistinct(), opinionAll.countDistinct()))
                .from(book)
                .innerJoin(passage).on(passage.book.eq(book))
                .innerJoin(opinionMine).on(opinionMine.passage.eq(passage)
                        .and(opinionMine.user.id.eq(userId))
                        .and(opinionMine.deletedAt.isNull()))
                .leftJoin(opinionAll).on(opinionAll.passage.eq(passage).and(opinionAll.deletedAt.isNull()))
                .groupBy(book.id, book.title, book.author, book.coverImageUrl)
                .orderBy(opinionMine.createdAt.max().desc(), book.id.asc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public long countMyLibraryBooks(Long userId) {
        QBook book = QBook.book;
        QPassage passage = QPassage.passage;
        QOpinion opinionMine = QOpinion.opinion;

        Long count = queryFactory
                .select(book.countDistinct())
                .from(book)
                .innerJoin(passage).on(passage.book.eq(book))
                .innerJoin(opinionMine).on(opinionMine.passage.eq(passage)
                        .and(opinionMine.user.id.eq(userId))
                        .and(opinionMine.deletedAt.isNull()))
                .fetchOne();
        return count != null ? count : 0L;
    }

    public Page<BookActivityProjection> findPopularBooks(Pageable pageable) {
        QBook book = QBook.book;
        QPassage passage = QPassage.passage;
        QOpinion opinion = QOpinion.opinion;

        List<BookActivityProjection> content = queryFactory
                .select(Projections.constructor(BookActivityProjection.class,
                        book.id, book.title, book.author, book.coverImageUrl,
                        passage.countDistinct(), opinion.countDistinct()))
                .from(book)
                .innerJoin(passage).on(passage.book.eq(book))
                .leftJoin(opinion).on(opinion.passage.eq(passage).and(opinion.deletedAt.isNull()))
                .groupBy(book.id, book.title, book.author, book.coverImageUrl)
                .orderBy(opinion.countDistinct().desc(), book.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, countBooksWithPassages());
    }

    // "최근에 남긴 책"은 Passage를 새로 만든 책이 아니라 흔적(Opinion)을 남긴 책 기준이다 (FR-WRITE-01).
    // 기존 Passage에 Opinion만 추가한 경우도 포함해야 하므로 Opinion을 기준으로 조회한다.
    public Page<Long> findRecentlyActiveBookIds(Long userId, Pageable pageable) {
        QOpinion opinion = QOpinion.opinion;
        QPassage passage = QPassage.passage;

        List<Long> content = queryFactory
                .select(passage.book.id)
                .from(opinion)
                .join(opinion.passage, passage)
                .where(opinion.user.id.eq(userId), opinion.deletedAt.isNull())
                .groupBy(passage.book.id)
                .orderBy(opinion.createdAt.max().desc(), passage.book.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(passage.book.countDistinct())
                .from(opinion)
                .join(opinion.passage, passage)
                .where(opinion.user.id.eq(userId), opinion.deletedAt.isNull())
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private long countBooksWithPassages() {
        QBook book = QBook.book;
        QPassage passage = QPassage.passage;

        Long count = queryFactory
                .select(book.countDistinct())
                .from(book)
                .innerJoin(passage).on(passage.book.eq(book))
                .fetchOne();
        return count != null ? count : 0L;
    }
}
