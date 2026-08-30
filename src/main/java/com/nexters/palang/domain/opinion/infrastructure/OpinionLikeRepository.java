package com.nexters.palang.domain.opinion.infrastructure;

import com.nexters.palang.domain.opinion.domain.OpinionLike;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpinionLikeRepository extends JpaRepository<OpinionLike, Long> {

    boolean existsByUserIdAndOpinionId(Long userId, Long opinionId);

    void deleteByUserIdAndOpinionId(Long userId, Long opinionId);

    // 관리자 유저 삭제(AdminUserService): 삭제 대상 의견에 달린 좋아요 전부(누가 눌렀는지 무관).
    void deleteAllByOpinionIdIn(List<Long> opinionIds);

    // 관리자 유저 삭제(AdminUserService): 이 유저가 남긴 나머지 좋아요.
    void deleteAllByUserId(Long userId);
}
