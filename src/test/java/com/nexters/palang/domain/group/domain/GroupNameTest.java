package com.nexters.palang.domain.group.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class GroupNameTest {

    @Test
    @DisplayName("앞뒤 공백은 제거하고 내부 공백은 유지한다")
    void stripsOuterWhitespaceAndKeepsInnerWhitespace() {
        assertThat(GroupName.normalize("  주말   독서 모임  ")).isEqualTo("주말   독서 모임");
    }

    @Test
    @DisplayName("보이는 글자 15자까지는 허용한다")
    void allowsFifteenGraphemes() {
        String fifteen = "가나다라마바사아자차카타파하거";

        assertThat(GroupName.normalize(fifteen)).isEqualTo(fifteen);
    }

    @Test
    @DisplayName("보이는 글자가 16자이면 예외가 발생한다")
    void rejectsSixteenGraphemes() {
        assertThatThrownBy(() -> GroupName.normalize("가나다라마바사아자차카타파하거너"))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.INVALID_GROUP_NAME);
    }

    @Test
    @DisplayName("이모지와 결합 문자는 UTF-16 길이가 아니라 보이는 글자 하나로 센다")
    void countsEmojiAndCombiningSequencesAsSingleCharacters() {
        // 가족 이모지(ZWJ 시퀀스, UTF-16 11자), 피부색 이모지(4자), 결합 문자 'é'(2자)를 각각 1자로 센다.
        String family = "👨‍👩‍👧‍👦";
        String thumbsUp = "👍🏽";
        String combinedE = "é";
        String name = family + thumbsUp + combinedE + "독서모임열두글자입니다요"; // 3 + 한글 12자 = 15

        assertThat(GroupName.graphemeLength(name)).isEqualTo(15);
        assertThat(GroupName.normalize(name)).isEqualTo(name);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n"})
    @DisplayName("비었거나 공백만 있으면 예외가 발생한다")
    void rejectsBlankName(String name) {
        assertThatThrownBy(() -> GroupName.normalize(name))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.INVALID_GROUP_NAME);
    }
}
