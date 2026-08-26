package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.passage.common.error.PassageErrorCode;
import com.nexters.palang.domain.passage.common.error.PassageException;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 페이지에서 대목(Passage)을 조회/수정/삭제하기 위한 서비스(issue #155 후속). 소프트 삭제된
// 대목도 정리 대상으로 볼 수 있어야 하므로 검색은 deletedAt과 무관하게 전부를 대상으로 한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPassageService {

    private final PassageRepository passageRepository;
    private final AdminCascadeDeleter cascadeDeleter;

    public Page<Passage> searchPassages(String keyword, Pageable pageable) {
        return passageRepository.findByQuotedTextContaining(keyword, pageable);
    }

    @Transactional
    public Passage updatePassage(Long passageId, String quotedText, int pageNumber, boolean isSpoiler) {
        Passage passage = getExistingPassage(passageId);
        passage.updateContent(quotedText, pageNumber);
        passage.changeSpoiler(isSpoiler);
        return passage;
    }

    // 대목 자체와 거기 달린 의견/댓글/데코/좋아요까지 전부 하드 삭제한다. 되돌릴 수 없다.
    @Transactional
    public void deletePassage(Long passageId) {
        getExistingPassage(passageId);
        cascadeDeleter.deletePassages(List.of(passageId));
    }

    private Passage getExistingPassage(Long passageId) {
        return passageRepository.findById(passageId)
                .orElseThrow(() -> new PassageException(PassageErrorCode.PASSAGE_NOT_FOUND));
    }
}
