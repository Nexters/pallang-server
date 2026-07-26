package com.nexters.palang.domain.opinion.infrastructure;

import com.nexters.palang.domain.book.domain.QBook;
import com.nexters.palang.domain.opinion.application.LikedOpinionProjection;
import com.nexters.palang.domain.opinion.application.MyOpinionProjection;
import com.nexters.palang.domain.opinion.domain.QOpinion;
import com.nexters.palang.domain.opinion.domain.QOpinionLike;
import com.nexters.palang.domain.passage.domain.QPassage;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OpinionQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<MyOpinionProjection> findMyOpinions(Long userId, Pageable pageable) {
        QOpinion opinion = QOpinion.opinion;
        QPassage passage = QPassage.passage;
        QBook book = QBook.book;

        List<MyOpinionProjection> content = queryFactory
                .select(Projections.constructor(MyOpinionProjection.class,
                        opinion.id, book.id, book.title, book.coverImageUrl,
                        passage.id, passage.quotedText, passage.pageNumber,
                        opinion.content, opinion.likeCount, opinion.createdAt))
                .from(opinion)
                .join(opinion.passage, passage)
                .join(passage.book, book)
                .where(opinion.user.id.eq(userId), opinion.deletedAt.isNull())
                .orderBy(opinion.createdAt.desc(), opinion.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(opinion.count())
                .from(opinion)
                .where(opinion.user.id.eq(userId), opinion.deletedAt.isNull())
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // liked_at(=OpinionLike.createdAt) 최신순. idx_likes_user_created(user_id, created_at) 인덱스와 정합.
    public Page<LikedOpinionProjection> findLikedOpinions(Long userId, Pageable pageable) {
        QOpinionLike opinionLike = QOpinionLike.opinionLike;
        QOpinion opinion = QOpinion.opinion;
        QPassage passage = QPassage.passage;
        QBook book = QBook.book;

        List<LikedOpinionProjection> content = queryFactory
                .select(Projections.constructor(LikedOpinionProjection.class,
                        opinion.id, book.id, book.title, book.coverImageUrl,
                        passage.id, passage.quotedText, passage.pageNumber,
                        opinion.content, opinion.likeCount, opinion.createdAt, opinionLike.createdAt))
                .from(opinionLike)
                .join(opinionLike.opinion, opinion)
                .join(opinion.passage, passage)
                .join(passage.book, book)
                .where(opinionLike.user.id.eq(userId), opinion.deletedAt.isNull())
                .orderBy(opinionLike.createdAt.desc(), opinionLike.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(opinionLike.count())
                .from(opinionLike)
                .join(opinionLike.opinion, opinion)
                .where(opinionLike.user.id.eq(userId), opinion.deletedAt.isNull())
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
