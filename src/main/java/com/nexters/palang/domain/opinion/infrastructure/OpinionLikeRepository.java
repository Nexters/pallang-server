package com.nexters.palang.domain.opinion.infrastructure;

import com.nexters.palang.domain.opinion.domain.OpinionLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpinionLikeRepository extends JpaRepository<OpinionLike, Long> {

    boolean existsByUserIdAndOpinionId(Long userId, Long opinionId);

    void deleteByUserIdAndOpinionId(Long userId, Long opinionId);
}
