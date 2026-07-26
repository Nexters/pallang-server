package com.nexters.palang.domain.decoration.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.decoration.domain.EffectType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DecorationMergeSelectorTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 1, 1, 0, 0);

    private static DecorationMergeCandidate candidate(long id, int start, int end, int likeCount, LocalDateTime createdAt) {
        return new DecorationMergeCandidate(id, start, end, EffectType.UNDERLINE, "#PRIMARY", likeCount, createdAt);
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of(
                        "겹치지 않으면 좋아요 많은 순으로 모두 채택된다",
                        List.of(
                                candidate(1, 0, 5, 3, T0),
                                candidate(2, 5, 10, 10, T0),
                                candidate(3, 10, 15, 1, T0)
                        ),
                        List.of(2L, 1L, 3L)
                ),
                Arguments.of(
                        "구간이 겹치면 좋아요가 적은 쪽이 제외된다",
                        List.of(
                                candidate(1, 0, 10, 10, T0),
                                candidate(2, 5, 15, 3, T0)
                        ),
                        List.of(1L)
                ),
                Arguments.of(
                        "좋아요 수가 같으면 최근에 작성된 흔적의 꾸밈이 우선한다",
                        List.of(
                                candidate(1, 0, 10, 5, T0),
                                candidate(2, 10, 20, 5, T0.plusDays(1))
                        ),
                        List.of(2L, 1L)
                ),
                Arguments.of(
                        "좋아요 수와 작성일이 모두 같으면 decoration.id가 작은 쪽이 결정적으로 우선한다",
                        List.of(
                                candidate(5, 0, 10, 5, T0),
                                candidate(2, 10, 20, 5, T0)
                        ),
                        List.of(2L, 5L)
                ),
                Arguments.of(
                        "겹치지 않는 후보가 4개 이상이어도 최대 3개까지만 채택된다",
                        List.of(
                                candidate(1, 0, 5, 4, T0),
                                candidate(2, 5, 10, 3, T0),
                                candidate(3, 10, 15, 2, T0),
                                candidate(4, 15, 20, 1, T0)
                        ),
                        List.of(1L, 2L, 3L)
                ),
                Arguments.of(
                        "후보가 없으면 빈 리스트를 반환한다",
                        List.<DecorationMergeCandidate>of(),
                        List.<Long>of()
                ),
                Arguments.of(
                        "겹치는 구간이 연쇄적이어도 이미 채택된 구간과 겹치는 후보만 제외한다",
                        List.of(
                                candidate(1, 0, 10, 10, T0),
                                candidate(2, 5, 15, 5, T0),
                                candidate(3, 12, 20, 3, T0)
                        ),
                        List.of(1L, 3L)
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    @DisplayName("겹침/동점자 케이스에 따라 결정적으로 최대 3개의 꾸밈이 채택된다")
    void select(String description, List<DecorationMergeCandidate> candidates, List<Long> expectedIdsInOrder) {
        List<DecorationMergeCandidate> selected = DecorationMergeSelector.select(candidates);

        assertThat(selected).extracting(DecorationMergeCandidate::decorationId)
                .containsExactlyElementsOf(expectedIdsInOrder);
    }
}
