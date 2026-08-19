package com.nexters.palang.domain.passage.application;

import com.nexters.palang.domain.book.application.BookOptionProjection;
import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.decoration.application.DecorationMergeCandidate;
import com.nexters.palang.domain.decoration.application.DecorationMergeSelector;
import com.nexters.palang.domain.decoration.infrastructure.DecorationQueryRepository;
import com.nexters.palang.domain.group.application.GroupAccessValidator;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.passage.common.error.PassageErrorCode;
import com.nexters.palang.domain.passage.common.error.PassageException;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageQueryRepository;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final BookRepository bookRepository;
    private final GroupAccessValidator groupAccessValidator;
    private final PassageRepository passageRepository;
    private final OpinionRepository opinionRepository;

    // 대목/흔적 보기 페이지 네비게이션: 발췌된 페이지 번호 목록 (스포일러 포함) + 헤더용 책 정보.
    // groupId가 있으면 그 모임 전용 대목만 대상으로 하며, 이때 userId는 모임원이어야 한다(비로그인이면 거절).
    public PageNumbersResult getPageNumbers(Long bookId, Long groupId, Long userId, Pageable pageable) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));
        if (groupId != null) {
            groupAccessValidator.validateMember(groupId, userId);
        }
        Page<Integer> pageNumbers = passageQueryRepository.findPageNumbers(bookId, groupId, pageable);
        return new PageNumbersResult(book, pageNumbers);
    }

    // 대목 전환: 특정 페이지의 대목들과 각 대목의 꾸밈 병합 결과.
    public List<Passage> getPassagesByPage(Long bookId, Long groupId, Long userId, int pageNumber) {
        validateBookExists(bookId);
        if (groupId != null) {
            groupAccessValidator.validateMember(groupId, userId);
        }
        return passageQueryRepository.findPassagesByPage(bookId, groupId, pageNumber);
    }

    // 내가 남긴 대목 목록: bookId 미지정 시 전체 도서, spoilerOnly=true 시 스포일러 대목만.
    public Page<MyPassageProjection> getMyPassages(Long userId, Long bookId, boolean spoilerOnly, Pageable pageable) {
        return passageQueryRepository.findMyPassages(userId, bookId, spoilerOnly, pageable);
    }

    // 스포일러 관리 화면의 "전체 책 보기" 드롭다운: 내가 스포일러로 남긴 대목이 있는 도서 목록.
    public Page<BookOptionProjection> getSpoilerBookOptions(Long userId, Pageable pageable) {
        return passageQueryRepository.findSpoilerBookOptions(userId, pageable);
    }

    // 대목 스포일러 설정 변경: 소유 기준은 findMyPassages와 동일하게 이 대목에 흔적을 남긴 사용자.
    // false→true 재설정도 허용한다(양방향 전환).
    @Transactional
    public Passage updateSpoiler(Long passageId, Long userId, boolean isSpoiler) {
        Passage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new PassageException(PassageErrorCode.PASSAGE_NOT_FOUND));
        if (passage.isDeleted()) {
            throw new PassageException(PassageErrorCode.PASSAGE_NOT_FOUND);
        }
        if (!opinionRepository.existsByPassageIdAndUserIdAndDeletedAtIsNull(passageId, userId)) {
            throw new PassageException(PassageErrorCode.PASSAGE_FORBIDDEN);
        }
        passage.changeSpoiler(isSpoiler);
        return passage;
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
