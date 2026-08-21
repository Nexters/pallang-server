package com.nexters.palang.domain.user.domain;

// 비로그인 사용자에게 보여줄 "내 의견" 미리보기(이슈 #120)는 하드코딩 값 대신 실제 계정의 실제
// 흔적/꾸밈 데이터를 그대로 보여준다 — 그래야 상세 화면의 Decoration(밑줄/동그라미 등)까지 자연스럽게
// 재사용된다. OpinionGuestSampleSeedRunner(생성)와 OpinionService(조회) 양쪽이 같은 식별자로 이
// 계정을 찾아야 하므로 상수를 이 중립적인 위치(user.domain)에 모아둔다.
public final class GuestSampleAccount {

    public static final SnsProvider SNS_PROVIDER = SnsProvider.KAKAO;
    public static final String SNS_ID = "guest-preview-sample-account";
    public static final String NICKNAME = "미리보기 계정";

    private GuestSampleAccount() {
    }
}
