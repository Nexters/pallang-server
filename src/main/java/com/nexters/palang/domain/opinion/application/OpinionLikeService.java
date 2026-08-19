package com.nexters.palang.domain.opinion.application;

import com.nexters.palang.domain.group.application.GroupAccessValidator;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.opinion.common.error.OpinionErrorCode;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.domain.OpinionLike;
import com.nexters.palang.domain.opinion.infrastructure.OpinionLikeRepository;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 좋아요 카운트 동기화(backend-plan.md §5.4): OpinionLike는 다른 엔티티(Opinion)의 캐시 값을
// 조율해야 하므로 정적 팩토리로는 표현할 수 없어 서비스 계층에서 같은 트랜잭션 안에 처리한다.
@Service
@RequiredArgsConstructor
public class OpinionLikeService {

    private final OpinionRepository opinionRepository;
    private final OpinionLikeRepository opinionLikeRepository;
    private final UserRepository userRepository;
    private final GroupAccessValidator groupAccessValidator;

    @Transactional
    public OpinionLikeResult toggleLike(Long userId, Long opinionId) {
        Opinion opinion = getExistingOpinion(opinionId);
        validateGroupAccess(opinion, userId);
        if (opinionLikeRepository.existsByUserIdAndOpinionId(userId, opinionId)) {
            return unlike(userId, opinion);
        }
        return like(userId, opinion);
    }

    private OpinionLikeResult like(Long userId, Opinion opinion) {
        User user = userRepository.getReferenceById(userId);
        opinionLikeRepository.save(OpinionLike.builder().user(user).opinion(opinion).build());
        opinion.increaseLikeCount();
        return new OpinionLikeResult(opinion.getId(), true, opinion.getLikeCount());
    }

    private OpinionLikeResult unlike(Long userId, Opinion opinion) {
        opinionLikeRepository.deleteByUserIdAndOpinionId(userId, opinion.getId());
        opinion.decreaseLikeCount();
        return new OpinionLikeResult(opinion.getId(), false, opinion.getLikeCount());
    }

    private Opinion getExistingOpinion(Long opinionId) {
        Opinion opinion = opinionRepository.findById(opinionId)
                .orElseThrow(() -> new OpinionException(OpinionErrorCode.OPINION_NOT_FOUND));
        if (opinion.isDeleted()) {
            throw new OpinionException(OpinionErrorCode.OPINION_NOT_FOUND);
        }
        return opinion;
    }

    // 흔적이 모임 전용이면(passage.group != null) 모임원만 좋아요를 남기거나 취소할 수 있다.
    private void validateGroupAccess(Opinion opinion, Long userId) {
        Group group = opinion.getPassage().getGroup();
        if (group != null) {
            groupAccessValidator.validateMember(group.getId(), userId);
        }
    }
}
