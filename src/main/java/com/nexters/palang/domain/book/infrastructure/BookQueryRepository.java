package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.application.BookActivityProjection;
import com.nexters.palang.domain.book.domain.QBook;
import com.nexters.palang.domain.opinion.domain.QOpinion;
import com.nexters.palang.domain.passage.domain.QPassage;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BookQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<BookActivityProjection> findCarouselBooks() {
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
                .orderBy(passage.createdAt.max().desc())
                .fetch();
    }

    public List<BookActivityProjection> findPopularBooks(int limit) {
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
                .orderBy(opinion.countDistinct().desc())
                .limit(limit)
                .fetch();
    }

    public List<Long> findRecentlyActiveBookIds(Long userId, int limit) {
        QPassage passage = QPassage.passage;

        return queryFactory
                .select(passage.book.id)
                .from(passage)
                .where(passage.creator.id.eq(userId))
                .groupBy(passage.book.id)
                .orderBy(passage.createdAt.max().desc())
                .limit(limit)
                .fetch();
    }
}
