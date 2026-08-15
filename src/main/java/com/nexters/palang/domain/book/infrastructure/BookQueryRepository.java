package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.application.BookActivityProjection;
import com.nexters.palang.domain.book.application.BookSearchProjection;
import com.nexters.palang.domain.book.application.BookSearchSort;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.QBook;
import com.nexters.palang.domain.opinion.domain.QOpinion;
import com.nexters.palang.domain.passage.domain.QPassage;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
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

    // 띄어쓰기 차이를 무시하고 검색할 수 있도록, 저장 시점에 미리 정규화(공백 제거 + 소문자)해둔
    // title_normalized 컬럼(인덱스 적용, Book#normalizeTitle 참고)을 기준으로 비교한다. 매 검색마다
    // title에 함수를 걸어 계산하지 않아도 되고, 자동완성처럼 반복 호출되는 경로에서 유리하다.
    // 흔적(Opinion)이 하나도 없는 도서는 결과에서 제외하므로 leftJoin이 아닌 innerJoin을 사용한다.
    public Page<BookSearchProjection> searchByTitle(String keyword, BookSearchSort sort, Pageable pageable) {
        QBook book = QBook.book;
        QPassage passage = QPassage.passage;
        QOpinion opinion = QOpinion.opinion;

        String normalizedKeyword = Book.normalize(keyword);

        List<BookSearchProjection> content = queryFactory
                .select(Projections.constructor(BookSearchProjection.class,
                        book.id, book.title, book.author, book.publisher, book.pageCount,
                        book.isbn, book.coverImageUrl, book.source,
                        passage.countDistinct(), opinion.countDistinct()))
                .from(book)
                .innerJoin(passage).on(passage.book.eq(book))
                .innerJoin(opinion).on(opinion.passage.eq(passage).and(opinion.deletedAt.isNull()))
                .where(book.titleNormalized.contains(normalizedKeyword))
                .groupBy(book.id, book.title, book.author, book.publisher, book.pageCount,
                        book.isbn, book.coverImageUrl, book.source)
                .orderBy(searchOrderSpecifiers(sort, book, opinion, normalizedKeyword))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(book.countDistinct())
                .from(book)
                .innerJoin(passage).on(passage.book.eq(book))
                .innerJoin(opinion).on(opinion.passage.eq(passage).and(opinion.deletedAt.isNull()))
                .where(book.titleNormalized.contains(normalizedKeyword))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // 자동완성으로 쓰일 때 관련도가 높은 결과(제목이 검색어로 시작하는 도서)가 먼저 보이도록,
    // 선택된 정렬 기준보다 접두어 일치 여부를 우선한다.
    private OrderSpecifier<?>[] searchOrderSpecifiers(
            BookSearchSort sort, QBook book, QOpinion opinion, String normalizedKeyword) {
        OrderSpecifier<Integer> prefixMatchFirst = new CaseBuilder()
                .when(book.titleNormalized.startsWith(normalizedKeyword)).then(0)
                .otherwise(1)
                .asc();

        OrderSpecifier<?>[] tieBreakers = switch (sort) {
            case NAME -> new OrderSpecifier[]{book.title.asc(), book.id.asc()};
            case RECENT -> new OrderSpecifier[]{opinion.createdAt.max().desc(), book.id.asc()};
            case OPINION -> new OrderSpecifier[]{opinion.countDistinct().desc(), book.id.asc()};
        };

        OrderSpecifier<?>[] result = new OrderSpecifier<?>[tieBreakers.length + 1];
        result[0] = prefixMatchFirst;
        System.arraycopy(tieBreakers, 0, result, 1, tieBreakers.length);
        return result;
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
