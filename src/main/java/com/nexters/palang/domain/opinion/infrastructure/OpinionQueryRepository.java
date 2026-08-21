package com.nexters.palang.domain.opinion.infrastructure;

import com.nexters.palang.domain.block.domain.QUserBlock;
import com.nexters.palang.domain.book.application.BookOptionProjection;
import com.nexters.palang.domain.book.domain.QBook;
import com.nexters.palang.domain.comment.domain.QComment;
import com.nexters.palang.domain.opinion.application.LikedOpinionProjection;
import com.nexters.palang.domain.opinion.application.MyOpinionProjection;
import com.nexters.palang.domain.opinion.application.OpinionSummaryProjection;
import com.nexters.palang.domain.opinion.domain.OpinionSortType;
import com.nexters.palang.domain.opinion.domain.QOpinion;
import com.nexters.palang.domain.opinion.domain.QOpinionLike;
import com.nexters.palang.domain.passage.domain.QPassage;
import com.nexters.palang.domain.user.domain.QUser;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
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

    // 흔적 목록(FR-OPINION-03): 최신순(기본)/좋아요순, 삭제된 흔적 제외.
    // liked: 현재 로그인 사용자 기준(비로그인이면 false), commentCount: 답글 포함/삭제 댓글 제외.
    public Page<OpinionSummaryProjection> findOpinions(
            Long passageId, OpinionSortType sortType, Pageable pageable, Long currentUserId) {
        QOpinion opinion = QOpinion.opinion;
        QUser user = QUser.user;
        QOpinionLike opinionLike = QOpinionLike.opinionLike;
        QComment comment = QComment.comment;
        QUserBlock userBlock = QUserBlock.userBlock;

        Expression<Boolean> likedExpression = currentUserId != null
                ? JPAExpressions.selectOne()
                        .from(opinionLike)
                        .where(opinionLike.opinion.id.eq(opinion.id), opinionLike.user.id.eq(currentUserId))
                        .exists()
                : Expressions.FALSE;

        Expression<Long> commentCountExpression = JPAExpressions
                .select(comment.count())
                .from(comment)
                .where(comment.opinion.id.eq(opinion.id), comment.deletedAt.isNull());

        // 차단(신고·차단 기능): 로그인 사용자가 차단한 작성자의 흔적은 피드에서 제외한다.
        BooleanExpression notBlockedByCurrentUser = currentUserId != null
                ? opinion.user.id.notIn(JPAExpressions
                        .select(userBlock.blocked.id)
                        .from(userBlock)
                        .where(userBlock.blocker.id.eq(currentUserId)))
                : null;

        List<OpinionSummaryProjection> content = queryFactory
                .select(Projections.constructor(OpinionSummaryProjection.class,
                        opinion.id, user.id, user.nickname, opinion.content, opinion.likeCount, opinion.createdAt,
                        likedExpression, commentCountExpression))
                .from(opinion)
                .join(opinion.user, user)
                .where(opinion.passage.id.eq(passageId), opinion.deletedAt.isNull(), notBlockedByCurrentUser)
                .orderBy(orderSpecifiers(sortType, opinion))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(opinion.count())
                .from(opinion)
                .where(opinion.passage.id.eq(passageId), opinion.deletedAt.isNull(), notBlockedByCurrentUser)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private OrderSpecifier<?>[] orderSpecifiers(OpinionSortType sortType, QOpinion opinion) {
        if (sortType == OpinionSortType.LIKES) {
            return new OrderSpecifier<?>[]{opinion.likeCount.desc(), opinion.createdAt.desc(), opinion.id.desc()};
        }
        return new OrderSpecifier<?>[]{opinion.createdAt.desc(), opinion.id.desc()};
    }

    public Page<MyOpinionProjection> findMyOpinions(Long userId, Long bookId, Pageable pageable) {
        QOpinion opinion = QOpinion.opinion;
        QPassage passage = QPassage.passage;
        QBook book = QBook.book;

        BooleanExpression bookFilter = bookId != null ? book.id.eq(bookId) : null;

        List<MyOpinionProjection> content = queryFactory
                .select(Projections.constructor(MyOpinionProjection.class,
                        opinion.id, book.id, book.title, book.author, book.coverImageUrl,
                        passage.id, passage.quotedText, passage.pageNumber,
                        opinion.content, opinion.likeCount, opinion.createdAt))
                .from(opinion)
                .join(opinion.passage, passage)
                .join(passage.book, book)
                .where(opinion.user.id.eq(userId), opinion.deletedAt.isNull(), bookFilter)
                .orderBy(opinion.createdAt.desc(), opinion.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(opinion.count())
                .from(opinion)
                .join(opinion.passage, passage)
                .join(passage.book, book)
                .where(opinion.user.id.eq(userId), opinion.deletedAt.isNull(), bookFilter)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // liked_at(=OpinionLike.createdAt) 최신순. idx_likes_user_created(user_id, created_at) 인덱스와 정합.
    public Page<LikedOpinionProjection> findLikedOpinions(Long userId, Long bookId, Pageable pageable) {
        QOpinionLike opinionLike = QOpinionLike.opinionLike;
        QOpinion opinion = QOpinion.opinion;
        QPassage passage = QPassage.passage;
        QBook book = QBook.book;
        QUser author = QUser.user;

        BooleanExpression bookFilter = bookId != null ? book.id.eq(bookId) : null;

        List<LikedOpinionProjection> content = queryFactory
                .select(Projections.constructor(LikedOpinionProjection.class,
                        opinion.id, book.id, book.title, book.author, book.coverImageUrl,
                        passage.id, passage.quotedText, passage.pageNumber,
                        opinion.content, author.nickname, opinion.likeCount, opinion.createdAt, opinionLike.createdAt))
                .from(opinionLike)
                .join(opinionLike.opinion, opinion)
                .join(opinion.passage, passage)
                .join(passage.book, book)
                .join(opinion.user, author)
                .where(opinionLike.user.id.eq(userId), opinion.deletedAt.isNull(), bookFilter)
                .orderBy(opinionLike.createdAt.desc(), opinionLike.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(opinionLike.count())
                .from(opinionLike)
                .join(opinionLike.opinion, opinion)
                .join(opinion.passage, passage)
                .join(passage.book, book)
                .where(opinionLike.user.id.eq(userId), opinion.deletedAt.isNull(), bookFilter)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // 좋아요·스포일러 관리 화면 공용 "전체 책 보기" 드롭다운: 활동(좋아요/스포일러) 최신순으로 중복 없이 책만 나열.
    public Page<BookOptionProjection> findLikedBookOptions(Long userId, Pageable pageable) {
        QOpinionLike opinionLike = QOpinionLike.opinionLike;
        QOpinion opinion = QOpinion.opinion;
        QPassage passage = QPassage.passage;
        QBook book = QBook.book;

        List<BookOptionProjection> content = queryFactory
                .select(Projections.constructor(BookOptionProjection.class, book.id, book.title))
                .from(opinionLike)
                .join(opinionLike.opinion, opinion)
                .join(opinion.passage, passage)
                .join(passage.book, book)
                .where(opinionLike.user.id.eq(userId), opinion.deletedAt.isNull())
                .groupBy(book.id, book.title)
                .orderBy(opinionLike.createdAt.max().desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(book.countDistinct())
                .from(opinionLike)
                .join(opinionLike.opinion, opinion)
                .join(opinion.passage, passage)
                .join(passage.book, book)
                .where(opinionLike.user.id.eq(userId), opinion.deletedAt.isNull())
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
