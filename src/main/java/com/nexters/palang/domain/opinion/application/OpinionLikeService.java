package com.nexters.palang.domain.opinion.application;

import com.nexters.palang.domain.opinion.common.error.OpinionErrorCode;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.domain.OpinionLike;
import com.nexters.palang.domain.opinion.infrastructure.OpinionLikeRepository;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 좋아요 카운트 동기화(backend-plan.md §5.4): opinions.like_count는 opinion_likes에 걸린
// DB 트리거(schema-h2.sql/schema-mysql.sql)가 갱신한다. 서비스는 OpinionLike 행만 생성/삭제하면 되지만,
// 트리거가 UPDATE한 값을 같은 트랜잭션의 영속성 컨텍스트가 모르는 stale read 문제가 있어
// flush 직후 refresh로 opinion을 다시 읽어야 정확한 likeCount를 응답할 수 있다.
@Service
@RequiredArgsConstructor
public class OpinionLikeService {

    private final OpinionRepository opinionRepository;
    private final OpinionLikeRepository opinionLikeRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    @Transactional
    public OpinionLikeResult toggleLike(Long userId, Long opinionId) {
        Opinion opinion = getExistingOpinion(opinionId);
        if (opinionLikeRepository.existsByUserIdAndOpinionId(userId, opinionId)) {
            return unlike(userId, opinion);
        }
        return like(userId, opinion);
    }

    private OpinionLikeResult like(Long userId, Opinion opinion) {
        User user = userRepository.getReferenceById(userId);
        opinionLikeRepository.save(OpinionLike.builder().user(user).opinion(opinion).build());
        return syncLikeState(opinion, true);
    }

    private OpinionLikeResult unlike(Long userId, Opinion opinion) {
        opinionLikeRepository.deleteByUserIdAndOpinionId(userId, opinion.getId());
        return syncLikeState(opinion, false);
    }

    private OpinionLikeResult syncLikeState(Opinion opinion, boolean liked) {
        opinionLikeRepository.flush();
        entityManager.refresh(opinion);
        return new OpinionLikeResult(opinion.getId(), liked, opinion.getLikeCount());
    }

    private Opinion getExistingOpinion(Long opinionId) {
        Opinion opinion = opinionRepository.findById(opinionId)
                .orElseThrow(() -> new OpinionException(OpinionErrorCode.OPINION_NOT_FOUND));
        if (opinion.isDeleted()) {
            throw new OpinionException(OpinionErrorCode.OPINION_NOT_FOUND);
        }
        return opinion;
    }
}
