package com.nexters.palang.domain.block.infrastructure;

import com.nexters.palang.domain.block.domain.QUserBlock;
import com.nexters.palang.domain.block.domain.UserBlock;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BlockQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<UserBlock> findBlockedUsers(Long blockerId, Pageable pageable) {
        QUserBlock userBlock = QUserBlock.userBlock;

        List<UserBlock> content = queryFactory
                .selectFrom(userBlock)
                .join(userBlock.blocked).fetchJoin()
                .where(userBlock.blocker.id.eq(blockerId))
                .orderBy(userBlock.createdAt.desc(), userBlock.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(userBlock.count())
                .from(userBlock)
                .where(userBlock.blocker.id.eq(blockerId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
