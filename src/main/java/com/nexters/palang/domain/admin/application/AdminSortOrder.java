package com.nexters.palang.domain.admin.application;

import org.springframework.data.domain.Sort;

// 관리자 목록 정렬. 작성(생성) 시각 기준이며, 같은 시각이면 id로 순서를 고정해 페이지를 넘겨도 항목이
// 중복되거나 빠지지 않게 한다.
public enum AdminSortOrder {
    LATEST(Sort.Direction.DESC),
    OLDEST(Sort.Direction.ASC),
    ;

    private final Sort.Direction direction;

    AdminSortOrder(Sort.Direction direction) {
        this.direction = direction;
    }

    public Sort toSort() {
        return Sort.by(direction, "createdAt").and(Sort.by(direction, "id"));
    }
}
