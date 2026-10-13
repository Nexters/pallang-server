package com.nexters.palang.domain.passage.domain;

import com.nexters.palang.domain.passage.common.error.PassageErrorCode;
import com.nexters.palang.domain.passage.common.error.PassageException;

// 내 흔적/좋아요/대목 목록을 대목의 소속(전역 공개 또는 특정 모임)으로 좁히는 조건 (이슈 #177).
// 같은 책이라도 전역 대목과 여러 모임의 대목이 섞여 있을 수 있어, 내 서재의 (책, 모임) 항목에서 들어간 책 상세는
// 그 범위의 것만 보여야 한다. 아무 조건도 없으면(ALL) 기존처럼 범위 구분 없이 전부 조회한다.
public record PassageScope(Long groupId, boolean globalOnly) {

    public static final PassageScope ALL = new PassageScope(null, false);

    public PassageScope {
        if (groupId != null && globalOnly) {
            throw new PassageException(PassageErrorCode.INVALID_PASSAGE_SCOPE);
        }
    }

    public static PassageScope of(Long groupId, boolean globalOnly) {
        return new PassageScope(groupId, globalOnly);
    }

    public boolean isGroup() {
        return groupId != null;
    }
}
