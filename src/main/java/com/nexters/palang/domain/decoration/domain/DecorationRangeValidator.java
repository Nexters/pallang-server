package com.nexters.palang.domain.decoration.domain;

import com.nexters.palang.domain.decoration.common.error.DecorationErrorCode;
import com.nexters.palang.domain.decoration.common.error.DecorationException;
import java.util.Comparator;
import java.util.List;

// 같은 Opinion 안에서 Decoration 영역은 1개씩만 가능하다 (FR-WRITE-09) → 구간 겹침을 생성 시점에 차단한다.
public final class DecorationRangeValidator {

    private DecorationRangeValidator() {
    }

    public static void validate(List<Decoration> decorations) {
        List<Decoration> sorted = decorations.stream()
                .sorted(Comparator.comparingInt(Decoration::getStartOffset))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            Decoration decoration = sorted.get(i);
            if (decoration.getEndOffset() <= decoration.getStartOffset()) {
                throw new DecorationException(DecorationErrorCode.INVALID_RANGE);
            }
            if (i > 0 && decoration.getStartOffset() < sorted.get(i - 1).getEndOffset()) {
                throw new DecorationException(DecorationErrorCode.OVERLAPPING_RANGE);
            }
        }
    }
}
