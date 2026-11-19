package com.nexters.palang.domain.group.application;

import org.springframework.data.domain.Page;

// 책 목록 조회 결과. statusFilter는 실제로 적용한 범위다 — 요청에 범위가 없으면 서버가 기본 탭을 골라
// (함께 읽는 책, 0권이면 완독한 책) 그 값을 돌려준다(L08). 탭 개수(readingCount/finishedCount)는 범위와 무관하게
// 모임 전체 기준이다.
public record GroupBookPage(
        GroupBookStatusFilter statusFilter, long readingCount, long finishedCount, Page<GroupBookView> books) {
}
