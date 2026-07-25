package com.nexters.palang.global.security;

public interface CurrentUserProvider {

    /**
     * 인증 Phase 이전에는 로그인 없이도 "인증 필요" 엔드포인트를 개발/테스트할 수 있도록
     * 구현체를 교체 가능한 지점으로 추상화한다. 인증 안 되어 있으면 LoginRequiredException.
     */
    Long getCurrentUserId();
}
