package com.nexters.palang.domain.passage.application;

import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.passage.infrastructure.PassageQueryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 유사 문장 판정(FR-WRITE-07): 같은 책의 인접 페이지(±1)에 정규화 해시가 일치하는 Passage가 있는지 조회한다.
@Component
@RequiredArgsConstructor
public class SimilarPassageFinder {

    private final PassageQueryRepository passageQueryRepository;
    private final BookRepository bookRepository;

    public List<SimilarPassageProjection> find(Long bookId, int pageNumber, String quotedText) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookException(BookErrorCode.BOOK_NOT_FOUND);
        }
        String normalizedHash = PassageNormalizer.normalizedHash(quotedText);
        return passageQueryRepository.findSimilarCandidates(bookId, pageNumber, normalizedHash);
    }
}
