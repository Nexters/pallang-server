package com.nexters.palang.global.security;

import java.util.Optional;

public interface CurrentUserProvider {

    /**
     * 인증 Phase 이전에는 로그인 없이도 "인증 필요" 엔드포인트를 개발/테스트할 수 있도록
     * 구현체를 교체 가능한 지점으로 추상화한다. 인증 안 되어 있으면 LoginRequiredException.
     */
    Long getCurrentUserId();

    // Soft Authentication(§4.2): 로그인 여부에 따라 같은 GET 엔드포인트가 다른 결과를 보여줘야 하는 경우 사용한다.
    // 비로그인이어도 예외 없이 빈 Optional을 반환한다.
    // 구현체(Header/JWT)를 건드리지 않고 인터페이스에서 한 번만 정의해 재사용한다.
    default Optional<Long> findCurrentUserId() {
        try {
            return Optional.of(getCurrentUserId());
        } catch (LoginRequiredException e) {
            return Optional.empty();
        }
    }
}
