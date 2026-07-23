package com.nexters.palang.domain.user.domain;

// DB의 ENUM('kakao', 'apple')과 대소문자가 다르지만, utf8mb4_unicode_ci 콜레이션은 대소문자를 구분하지 않아 매칭된다.
public enum SnsProvider {
    KAKAO,
    APPLE
}
