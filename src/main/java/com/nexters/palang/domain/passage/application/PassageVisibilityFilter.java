package com.nexters.palang.domain.passage.application;

import com.nexters.palang.domain.book.domain.ReadingStatus;
import com.nexters.palang.domain.book.domain.UserBookStatus;
import com.nexters.palang.domain.book.infrastructure.UserBookStatusRepository;
import com.nexters.palang.domain.passage.domain.QPassage;
import com.nexters.palang.domain.passage.infrastructure.PassageQueryRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 읽기 상태 기반 노출 필터(FR-WRITE-08, backend-plan.md §5.7):
// READING  -> pageNumber <= currentPage
// PLANNED / 미설정 / 비로그인 -> 가장 작은 페이지(첫 대목)만
//
// 스포일러(isSpoiler)는 여기서 걸러내지 않는다. 스포일러 대목도 "존재 자체"는 이 필터를 그대로 통과하고,
// 실제 내용(quotedText/꾸밈)을 가리는 건 응답을 만드는 쪽(PassageResponse)의 책임이다 (FR-VIEW-03 블러+확인 버튼).
@Component
@RequiredArgsConstructor
public class PassageVisibilityFilter {

    private final UserBookStatusRepository userBookStatusRepository;
    private final PassageQueryRepository passageQueryRepository;

    public BooleanExpression build(Long bookId, Long currentUserId) {
        return pageNumberCondition(bookId, currentUserId);
    }

    // 노출 가능한 대목 중 가장 작은 페이지 번호 (책에 대목이 하나도 없으면 empty).
    // 비로그인 사용자가 이 페이지가 아닌 다른 페이지를 요청했는지 판단하는 데 쓰인다(§4.2, FR-OPINION-08).
    public Optional<Integer> firstVisiblePageNumber(Long bookId) {
        return Optional.ofNullable(passageQueryRepository.findFirstVisiblePageNumber(bookId));
    }

    private BooleanExpression pageNumberCondition(Long bookId, Long currentUserId) {
        QPassage passage = QPassage.passage;
        Optional<UserBookStatus> readingStatus = currentUserId == null
                ? Optional.empty()
                : userBookStatusRepository.findByUserIdAndBookId(currentUserId, bookId);

        if (readingStatus.isPresent()
                && readingStatus.get().getStatus() == ReadingStatus.READING
                && readingStatus.get().getCurrentPage() != null) {
            return passage.pageNumber.loe(readingStatus.get().getCurrentPage());
        }

        return firstVisiblePageNumber(bookId)
                .map(passage.pageNumber::eq)
                .orElseGet(() -> Expressions.asBoolean(false).isTrue());
    }
}
