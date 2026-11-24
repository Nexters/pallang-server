package com.nexters.palang.domain.group.domain;

import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import java.util.regex.Pattern;

// 모임명 규칙: 앞뒤 공백을 제거한 뒤 사용자 인식 문자(grapheme) 기준 1~15자. 내부 공백은 유지하고, 중복 이름은
// 허용한다. String.length()는 UTF-16 코드 유닛을 세서 이모지/결합 문자를 2자 이상으로 계산하므로 쓰지 않는다.
public final class GroupName {

    public static final int MAX_GRAPHEME_LENGTH = 15;
    // DB 컬럼 길이(문자 수). 15 grapheme이라도 ZWJ 이모지 등은 코드 포인트가 훨씬 많을 수 있어 여유를 둔다.
    public static final int COLUMN_LENGTH = 100;

    private static final Pattern GRAPHEME = Pattern.compile("\\X");

    private GroupName() {
    }

    public static String normalize(String rawName) {
        if (rawName == null) {
            throw new GroupException(GroupErrorCode.INVALID_GROUP_NAME);
        }
        String trimmed = rawName.strip();
        long length = graphemeLength(trimmed);
        if (length < 1 || length > MAX_GRAPHEME_LENGTH || trimmed.length() > COLUMN_LENGTH) {
            throw new GroupException(GroupErrorCode.INVALID_GROUP_NAME);
        }
        return trimmed;
    }

    static long graphemeLength(String value) {
        return GRAPHEME.matcher(value).results().count();
    }
}
