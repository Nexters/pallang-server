package com.nexters.palang.global.common.response;

import org.springframework.data.domain.Page;

public record PageInfo(int page, int size, long totalElements, int totalPages, boolean hasNext) {

    public static PageInfo from(Page<?> page) {
        return new PageInfo(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }
}
