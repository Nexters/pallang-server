package com.nexters.palang.domain.passage.application;

import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.group.application.GroupAccessValidator;
import com.nexters.palang.domain.passage.infrastructure.PassageQueryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 유사 문장 판정(FR-WRITE-07): 같은 책의 인접 페이지(±1)에 정규화 해시가 일치하는 Passage가 있는지 조회한다.
// groupId가 있으면 그 모임 스코프 안에서만 비교한다(전역 공개 대목과는 서로 병합 후보가 되지 않는다).
@Component
@RequiredArgsConstructor
public class SimilarPassageFinder {

    private final PassageQueryRepository passageQueryRepository;
    private final BookRepository bookRepository;
    private final GroupAccessValidator groupAccessValidator;

    public List<SimilarPassageProjection> find(Long bookId, int pageNumber, String quotedText, Long groupId, Long userId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookException(BookErrorCode.BOOK_NOT_FOUND);
        }
        if (groupId != null) {
            groupAccessValidator.validateMember(groupId, userId);
        }
        String normalizedHash = PassageNormalizer.normalizedHash(quotedText);
        return passageQueryRepository.findSimilarCandidates(bookId, pageNumber, normalizedHash, groupId);
    }
}
