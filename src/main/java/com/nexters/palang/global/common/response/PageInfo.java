package com.nexters.palang.global.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

public record PageInfo(
        @Schema(example = "0") int page,
        @Schema(example = "20") int size,
        @Schema(example = "42") long totalElements,
        @Schema(example = "3") int totalPages,
        @Schema(example = "true") boolean hasNext
) {

    public static PageInfo from(Page<?> page) {
        return new PageInfo(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }
}
