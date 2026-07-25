package com.nexters.palang.domain.comment.infrastructure;

import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.comment.domain.QComment;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Comment> findRootComments(Long opinionId, Pageable pageable) {
        QComment comment = QComment.comment;

        List<Comment> content = queryFactory
                .selectFrom(comment)
                .where(comment.opinion.id.eq(opinionId), comment.parentComment.isNull())
                .orderBy(comment.createdAt.asc(), comment.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.opinion.id.eq(opinionId), comment.parentComment.isNull())
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // 페이지 하나에 담긴 원댓글들의 답글을 미리보기+개수 계산용으로 한 번에 조회한다 (MVP 규모에서는 N+1보다 저렴하다).
    public List<Comment> findRepliesByParentIds(List<Long> parentIds) {
        if (parentIds.isEmpty()) {
            return List.of();
        }
        QComment comment = QComment.comment;

        return queryFactory
                .selectFrom(comment)
                .where(comment.parentComment.id.in(parentIds))
                .orderBy(comment.parentComment.id.asc(), comment.createdAt.asc(), comment.id.asc())
                .fetch();
    }

    public Page<Comment> findReplies(Long parentCommentId, Pageable pageable) {
        QComment comment = QComment.comment;

        List<Comment> content = queryFactory
                .selectFrom(comment)
                .where(comment.parentComment.id.eq(parentCommentId))
                .orderBy(comment.createdAt.asc(), comment.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.parentComment.id.eq(parentCommentId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
