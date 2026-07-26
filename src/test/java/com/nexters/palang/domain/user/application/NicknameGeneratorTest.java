package com.nexters.palang.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class NicknameGeneratorTest {

    private final NicknameGenerator nicknameGenerator = new NicknameGenerator();

    @RepeatedTest(20)
    @DisplayName("기본 닉네임은 형용사와 명사를 이어붙인 형태다")
    void generateBaseProducesAdjectiveNounCombination() {
        String base = nicknameGenerator.generateBase();

        assertThat(base).isNotBlank();
        assertThat(base).doesNotContainAnyWhitespaces();
    }

    @Test
    @DisplayName("접미사를 붙이면 기본 닉네임 뒤에 숫자가 그대로 붙는다")
    void withSuffixAppendsNumberToBase() {
        String result = nicknameGenerator.withSuffix("고요한책갈피", 7);

        assertThat(result).isEqualTo("고요한책갈피7");
    }
}
