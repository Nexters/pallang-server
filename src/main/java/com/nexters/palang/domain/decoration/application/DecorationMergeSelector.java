package com.nexters.palang.domain.decoration.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// 꾸밈 병합 노출(FR-VIEW-03, backend-plan.md §5.6): 한 Passage에 달린 모든 Decoration 중
// 좋아요 많은 순으로 최대 3개를 겹치지 않게 뽑는다. 순수 함수라 파라미터화 테스트로 검증하기 쉽다.
public final class DecorationMergeSelector {

    private static final int MAX_SELECTED = 3;

    // 정렬: 좋아요 수 DESC, 흔적 작성일 DESC, decoration.id ASC.
    // 3순위 "랜덤"(기획서) 대신 decoration.id를 결정적 tiebreaker로 써서 캐시 무효화/사용자 혼란을 피한다.
    private static final Comparator<DecorationMergeCandidate> PRIORITY_ORDER =
            Comparator.comparingInt(DecorationMergeCandidate::likeCount).reversed()
                    .thenComparing(Comparator.comparing(DecorationMergeCandidate::opinionCreatedAt).reversed())
                    .thenComparing(DecorationMergeCandidate::decorationId);

    private DecorationMergeSelector() {
    }

    public static List<DecorationMergeCandidate> select(List<DecorationMergeCandidate> candidates) {
        List<DecorationMergeCandidate> sorted = candidates.stream().sorted(PRIORITY_ORDER).toList();

        List<DecorationMergeCandidate> selected = new ArrayList<>();
        for (DecorationMergeCandidate candidate : sorted) {
            if (selected.size() == MAX_SELECTED) {
                break;
            }
            if (selected.stream().noneMatch(accepted -> overlaps(accepted, candidate))) {
                selected.add(candidate);
            }
        }
        return selected;
    }

    private static boolean overlaps(DecorationMergeCandidate a, DecorationMergeCandidate b) {
        return a.startOffset() < b.endOffset() && b.startOffset() < a.endOffset();
    }
}
