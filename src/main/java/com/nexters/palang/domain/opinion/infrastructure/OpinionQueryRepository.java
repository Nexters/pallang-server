package com.nexters.palang.domain.opinion.infrastructure;

import com.nexters.palang.domain.opinion.application.OpinionSummaryProjection;
import com.nexters.palang.domain.opinion.domain.OpinionSortType;
import com.nexters.palang.domain.opinion.domain.QOpinion;
import com.nexters.palang.domain.user.domain.QUser;
import com.querydsl.core.types.OrderSpecifier;
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

    // 흔적 목록(FR-OPINION-03): 최신순(기본)/좋아요순, 삭제된 흔적 제외.
    public Page<OpinionSummaryProjection> findOpinions(Long passageId, OpinionSortType sortType, Pageable pageable) {
        QOpinion opinion = QOpinion.opinion;
        QUser user = QUser.user;

        List<OpinionSummaryProjection> content = queryFactory
                .select(Projections.constructor(OpinionSummaryProjection.class,
                        opinion.id, user.id, user.nickname, opinion.content, opinion.likeCount, opinion.createdAt))
                .from(opinion)
                .join(opinion.user, user)
                .where(opinion.passage.id.eq(passageId), opinion.deletedAt.isNull())
                .orderBy(orderSpecifiers(sortType, opinion))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(opinion.count())
                .from(opinion)
                .where(opinion.passage.id.eq(passageId), opinion.deletedAt.isNull())
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private OrderSpecifier<?>[] orderSpecifiers(OpinionSortType sortType, QOpinion opinion) {
        if (sortType == OpinionSortType.LIKES) {
            return new OrderSpecifier<?>[]{opinion.likeCount.desc(), opinion.createdAt.desc(), opinion.id.desc()};
        }
        return new OrderSpecifier<?>[]{opinion.createdAt.desc(), opinion.id.desc()};
    }
}
