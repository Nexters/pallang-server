package com.nexters.palang.domain.decoration.infrastructure;

import com.nexters.palang.domain.decoration.application.DecorationMergeCandidate;
import com.nexters.palang.domain.decoration.domain.QDecoration;
import com.nexters.palang.domain.opinion.domain.QOpinion;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DecorationQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 정렬은 DecorationMergeSelector가 담당하므로 여기서는 후보만 모아온다 (삭제된 Opinion 제외).
    public List<DecorationMergeCandidate> findMergeCandidates(Long passageId) {
        QDecoration decoration = QDecoration.decoration;
        QOpinion opinion = QOpinion.opinion;

        return queryFactory
                .select(Projections.constructor(DecorationMergeCandidate.class,
                        decoration.id, decoration.startOffset, decoration.endOffset,
                        decoration.effectType, decoration.color,
                        opinion.likeCount, opinion.createdAt))
                .from(decoration)
                .join(decoration.opinion, opinion)
                .where(opinion.passage.id.eq(passageId), opinion.deletedAt.isNull())
                .fetch();
    }
}
