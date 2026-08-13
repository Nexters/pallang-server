package com.nexters.palang.domain.user.application;

import java.security.SecureRandom;
import java.util.List;
import org.springframework.stereotype.Component;

// FR-AUTH-04: 형용사 30 x 명사 30 랜덤 조합(900개) + 숫자 접미사(1~100) = 90,000개.
// 유니크 제약 충돌 처리(저장 시도 -> 재시도)는 UserRegistrationService가 담당한다.
@Component
public class NicknameGenerator {

    public static final int MAX_SUFFIX_ATTEMPTS = 100;

    private static final List<String> ADJECTIVES = List.of(
            "고요한", "다정한", "따스한", "느긋한", "단단한", "반짝이는", "맑은", "포근한", "조용한", "용감한",
            "성실한", "섬세한", "담백한", "신나는", "차분한", "자유로운", "깊은", "작은", "푸른", "은은한",
            "따뜻한", "빛나는", "소박한", "여유로운", "성숙한", "솔직한", "정다운", "특별한", "평온한", "부지런한"
    );

    private static final List<String> NOUNS = List.of(
            "책갈피", "문장", "연필", "서재", "별빛", "달빛", "숲길", "잉크", "종이", "다락방",
            "찻잔", "산책자", "독서등", "노트", "고래", "나무", "파도", "구름", "여우", "다람쥐",
            "등불", "우체통", "시계", "지도", "여정", "쉼표", "낙엽", "창가", "이야기", "여백"
    );

    private final SecureRandom random = new SecureRandom();

    public String generateBase() {
        return pick(ADJECTIVES) + pick(NOUNS);
    }

    public String withSuffix(String base, int suffix) {
        return base + suffix;
    }

    private String pick(List<String> words) {
        return words.get(random.nextInt(words.size()));
    }
}
