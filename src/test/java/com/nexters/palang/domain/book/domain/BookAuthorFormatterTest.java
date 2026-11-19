package com.nexters.palang.domain.book.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class BookAuthorFormatterTest {

    @ParameterizedTest(name = "[{index}] \"{0}\" -> \"{1}\"")
    @DisplayName("작가명에서 역할 표기를 떼고 번역·감수 등 저자가 아닌 이름은 뺀다")
    @CsvSource(delimiter = '|', value = {
            // 알라딘 형식
            "양귀자 (지은이)|양귀자",
            "알베르 카뮈 (지은이), 김화영 (옮긴이)|알베르 카뮈",
            "김영하, 김연수 (지은이)|김영하, 김연수",
            "김영하, 김연수 (지은이), 홍길동 (옮긴이)|김영하, 김연수",
            "앤서니 브라운 (글·그림), 장미란 (옮긴이)|앤서니 브라운",
            "이수지 (글), 이명애 (그림)|이수지, 이명애",
            "홍길동 (엮은이), 김철수 (감수), 이영희 (해설)|홍길동",
            // 카카오 형식 / 직접 입력은 그대로
            "양귀자|양귀자",
            "김영하, 김연수|김영하, 김연수",
            "J. R. R. 톨킨|J. R. R. 톨킨",
            // 번역자만 표기된 예외: 빈 값 대신 이름을 보여준다
            "김화영 (옮긴이)|김화영",
            // 공백 정리
            "'  양귀자 (지은이)  '|양귀자",
    })
    void toDisplayName(String author, String expected) {
        assertThat(BookAuthorFormatter.toDisplayName(author)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("작가명이 비어 있으면 그대로 반환한다")
    void toDisplayNameReturnsBlankAsIs(String author) {
        assertThat(BookAuthorFormatter.toDisplayName(author)).isEqualTo(author);
    }
}
