package com.nexters.palang.domain.comment.infrastructure;

import com.nexters.palang.domain.block.domain.QUserBlock;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.comment.domain.QComment;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Comment> findRootComments(Long opinionId, Pageable pageable, Long currentUserId) {
        QComment comment = QComment.comment;
        BooleanExpression notBlocked = notBlockedByCurrentUser(comment, currentUserId);

        List<Comment> content = queryFactory
                .selectFrom(comment)
                .join(comment.user).fetchJoin()
                .where(comment.opinion.id.eq(opinionId), comment.parentComment.isNull(), notBlocked)
                .orderBy(comment.createdAt.asc(), comment.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.opinion.id.eq(opinionId), comment.parentComment.isNull(), notBlocked)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    public Map<Long, Long> countRepliesByParentIds(List<Long> parentIds, Long currentUserId) {
        if (parentIds.isEmpty()) {
            return Map.of();
        }
        QComment comment = QComment.comment;

        List<Tuple> counts = queryFactory
                .select(comment.parentComment.id, comment.count())
                .from(comment)
                .where(comment.parentComment.id.in(parentIds), notBlockedByCurrentUser(comment, currentUserId))
                .groupBy(comment.parentComment.id)
                .fetch();

        Map<Long, Long> countsByParentId = new LinkedHashMap<>();
        for (Tuple count : counts) {
            countsByParentId.put(count.get(comment.parentComment.id), count.get(comment.count()));
        }
        return countsByParentId;
    }

    // 답글이 아무리 많아도 부모마다 최대 previewSize개만 DB에서 LIMIT으로 가져온다 (전체 답글을 로드하지 않는다).
    public Map<Long, List<Comment>> findReplyPreviewsByParentIds(List<Long> parentIds, int previewSize, Long currentUserId) {
        if (parentIds.isEmpty()) {
            return Map.of();
        }
        QComment comment = QComment.comment;
        BooleanExpression notBlocked = notBlockedByCurrentUser(comment, currentUserId);

        Map<Long, List<Comment>> previewsByParentId = new LinkedHashMap<>();
        for (Long parentId : parentIds) {
            List<Comment> preview = queryFactory
                    .selectFrom(comment)
                    .join(comment.user).fetchJoin()
                    .where(comment.parentComment.id.eq(parentId), notBlocked)
                    .orderBy(comment.createdAt.asc(), comment.id.asc())
                    .limit(previewSize)
                    .fetch();
            previewsByParentId.put(parentId, preview);
        }
        return previewsByParentId;
    }

    public Page<Comment> findReplies(Long parentCommentId, Pageable pageable, Long currentUserId) {
        QComment comment = QComment.comment;
        BooleanExpression notBlocked = notBlockedByCurrentUser(comment, currentUserId);

        List<Comment> content = queryFactory
                .selectFrom(comment)
                .join(comment.user).fetchJoin()
                .where(comment.parentComment.id.eq(parentCommentId), notBlocked)
                .orderBy(comment.createdAt.asc(), comment.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.parentComment.id.eq(parentCommentId), notBlocked)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // 차단(신고·차단 기능): 로그인 사용자가 차단한 작성자의 댓글/답글은 목록에서 제외한다.
    private BooleanExpression notBlockedByCurrentUser(QComment comment, Long currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        QUserBlock userBlock = QUserBlock.userBlock;
        return comment.user.id.notIn(JPAExpressions
                .select(userBlock.blocked.id)
                .from(userBlock)
                .where(userBlock.blocker.id.eq(currentUserId)));
    }
}
