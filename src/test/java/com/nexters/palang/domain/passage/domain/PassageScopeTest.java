package com.nexters.palang.domain.passage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.domain.passage.common.error.PassageErrorCode;
import com.nexters.palang.domain.passage.common.error.PassageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PassageScopeTest {

    @Test
    @DisplayName("groupId와 globalOnly를 함께 지정하면 예외가 발생한다")
    void ofFailsWhenGroupIdAndGlobalOnlyBothGiven() {
        // when & then
        assertThatThrownBy(() -> PassageScope.of(13L, true))
                .isInstanceOf(PassageException.class)
                .extracting("errorCode")
                .isEqualTo(PassageErrorCode.INVALID_PASSAGE_SCOPE);
    }

    @Test
    @DisplayName("아무 조건도 주지 않으면 범위 구분 없는 전체 조회(ALL)와 같다")
    void ofReturnsAllWhenNoConditionGiven() {
        // when
        PassageScope scope = PassageScope.of(null, false);

        // then
        assertThat(scope).isEqualTo(PassageScope.ALL);
        assertThat(scope.isGroup()).isFalse();
    }

    @Test
    @DisplayName("groupId를 지정하면 모임 범위로 취급한다")
    void ofReturnsGroupScopeWhenGroupIdGiven() {
        // when
        PassageScope scope = PassageScope.of(13L, false);

        // then
        assertThat(scope.isGroup()).isTrue();
        assertThat(scope.groupId()).isEqualTo(13L);
    }
}
