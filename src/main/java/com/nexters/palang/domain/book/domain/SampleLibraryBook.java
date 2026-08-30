package com.nexters.palang.domain.book.domain;

// 비로그인 사용자와, 로그인했지만 서재에 책이 하나도 없는 계정이 홈에서 보는 고정 샘플 도서
// (기획 확정, 이슈 #119)의 시드 값. ISBN으로 조회해 이 책의 실제 book row(SampleLibraryBookSeeder가
// 앱 시작 시 없으면 생성)를 찾아 그 환경의 실제 id를 쓴다 — 환경마다 로컬 PK가 다를 수 있어, PK를
// 직접 하드코딩하면 그 PK가 없는 환경(예: 이 책이 한 번도 만들어진 적 없는 prod)에서 404가 난다.
public final class SampleLibraryBook {

    public static final String ISBN = "9791194891178";
    public static final String TITLE = "빵충 사육 준수 사항";
    public static final String AUTHOR = "김혜영 (지은이)";
    public static final String PUBLISHER = "안전가옥";
    public static final int PAGE_COUNT = 388;
    public static final String COVER_IMAGE_URL =
            "https://image.aladin.co.kr/product/39872/66/cover200/k242130313_1.jpg";

    private SampleLibraryBook() {
    }
}
