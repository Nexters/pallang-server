package com.nexters.palang.domain.passage.application;

import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.decoration.application.DecorationMergeCandidate;
import com.nexters.palang.domain.decoration.application.DecorationMergeSelector;
import com.nexters.palang.domain.decoration.infrastructure.DecorationQueryRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageQueryRepository;
import com.nexters.palang.global.security.LoginRequiredException;
import com.querydsl.core.types.dsl.BooleanExpression;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PassageService {

    private final PassageQueryRepository passageQueryRepository;
    private final DecorationQueryRepository decorationQueryRepository;
    private final PassageVisibilityFilter passageVisibilityFilter;
    private final BookRepository bookRepository;

    // 대목/흔적 보기 페이지 네비게이션(FR-VIEW-02): 읽기상태 노출 필터를 만족하는 페이지 번호 목록 (스포일러 포함).
    public Page<Integer> getVisiblePageNumbers(Long bookId, Long currentUserId, Pageable pageable) {
        validateBookExists(bookId);
        BooleanExpression visibilityFilter = passageVisibilityFilter.build(bookId, currentUserId);
        return passageQueryRepository.findVisiblePageNumbers(bookId, visibilityFilter, pageable);
    }

    // 대목 전환(FR-VIEW-03): 특정 페이지의 대목들과 각 대목의 꾸밈 병합 결과.
    // 비로그인 사용자가 첫 대목이 아닌 페이지를 요청하면 로그인을 유도한다(FR-OPINION-08, backend-plan.md §4.2).
    public List<Passage> getVisiblePassagesByPage(Long bookId, int pageNumber, Long currentUserId) {
        validateBookExists(bookId);
        if (currentUserId == null) {
            Optional<Integer> firstPage = passageVisibilityFilter.firstVisiblePageNumber(bookId);
            if (firstPage.isPresent() && pageNumber != firstPage.get()) {
                throw new LoginRequiredException();
            }
        }
        BooleanExpression visibilityFilter = passageVisibilityFilter.build(bookId, currentUserId);
        return passageQueryRepository.findVisiblePassagesByPage(bookId, pageNumber, visibilityFilter);
    }

    public Map<Long, List<DecorationMergeCandidate>> getMergedDecorationsByPassageId(List<Passage> passages) {
        Map<Long, List<DecorationMergeCandidate>> mergedByPassageId = new LinkedHashMap<>();
        for (Passage passage : passages) {
            List<DecorationMergeCandidate> candidates = decorationQueryRepository.findMergeCandidates(passage.getId());
            mergedByPassageId.put(passage.getId(), DecorationMergeSelector.select(candidates));
        }
        return mergedByPassageId;
    }

    private void validateBookExists(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookException(BookErrorCode.BOOK_NOT_FOUND);
        }
    }
}
