package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.opinion.common.error.OpinionErrorCode;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 페이지에서 의견(Opinion)을 조회/수정/삭제하기 위한 서비스(issue #155 후속). OpinionService의
// modifyOpinion/removeOpinion은 작성자 본인만 호출할 수 있어(validateOwner) 관리자 용도로 재사용할 수
// 없다 — 소유권 검사를 우회하는 별도 경로가 필요하다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOpinionService {

    private final OpinionRepository opinionRepository;
    private final PassageRepository passageRepository;
    private final AdminCascadeDeleter cascadeDeleter;

    public Page<Opinion> searchOpinions(String keyword, Pageable pageable) {
        return opinionRepository.findByContentContaining(keyword, pageable);
    }

    @Transactional
    public Opinion updateOpinion(Long opinionId, String content) {
        Opinion opinion = getExistingOpinion(opinionId);
        opinion.updateContent(content);
        return opinion;
    }

    // 의견과 거기 달린 댓글/데코/좋아요를 하드 삭제한다. 그 대목에 남은 살아있는 의견이 더 없으면
    // (OpinionService.removeOpinion과 동일한 규칙) 대목도 함께 소프트 삭제한다. 되돌릴 수 없다.
    @Transactional
    public void deleteOpinion(Long opinionId) {
        Opinion opinion = getExistingOpinion(opinionId);
        Long passageId = opinion.getPassage().getId();
        boolean wasLastLiveOpinion = !opinionRepository.existsByPassageIdAndDeletedAtIsNullAndIdNot(passageId, opinionId);

        cascadeDeleter.deleteOpinions(List.of(opinionId));

        if (wasLastLiveOpinion) {
            passageRepository.findById(passageId).ifPresent(passage -> passage.delete());
        }
    }

    private Opinion getExistingOpinion(Long opinionId) {
        return opinionRepository.findWithAssociationsById(opinionId)
                .orElseThrow(() -> new OpinionException(OpinionErrorCode.OPINION_NOT_FOUND));
    }
}
