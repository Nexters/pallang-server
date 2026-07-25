package com.nexters.palang.domain.passage.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PassageNormalizerTest {

    @Test
    @DisplayName("공백만 다른 두 문장은 같은 정규화 해시를 갖는다")
    void whitespaceDifferenceProducesSameHash() {
        String hash1 = PassageNormalizer.normalizedHash("나는 오늘도 걷는다");
        String hash2 = PassageNormalizer.normalizedHash("나는  오늘도   걷는다");

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("구두점만 다른 두 문장은 같은 정규화 해시를 갖는다")
    void punctuationDifferenceProducesSameHash() {
        String hash1 = PassageNormalizer.normalizedHash("나는 오늘도 걷는다");
        String hash2 = PassageNormalizer.normalizedHash("나는, 오늘도 걷는다!");

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("NFC/NFD 정규화 형태가 다른 두 문장은 같은 정규화 해시를 갖는다")
    void unicodeNormalizationFormDifferenceProducesSameHash() {
        String nfc = Normalizer.normalize("나는 오늘도 걷는다", Normalizer.Form.NFC);
        String nfd = Normalizer.normalize("나는 오늘도 걷는다", Normalizer.Form.NFD);

        assertThat(PassageNormalizer.normalizedHash(nfc)).isEqualTo(PassageNormalizer.normalizedHash(nfd));
    }

    @Test
    @DisplayName("실제 내용이 다른 두 문장은 다른 정규화 해시를 갖는다")
    void differentContentProducesDifferentHash() {
        String hash1 = PassageNormalizer.normalizedHash("나는 오늘도 걷는다");
        String hash2 = PassageNormalizer.normalizedHash("나는 오늘도 뛴다");

        assertThat(hash1).isNotEqualTo(hash2);
    }
}
