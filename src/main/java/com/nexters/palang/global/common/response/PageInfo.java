package com.nexters.palang.global.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

public record PageInfo(
        @Schema(example = "0", requiredMode = Schema.RequiredMode.REQUIRED) int page,
        @Schema(example = "20", requiredMode = Schema.RequiredMode.REQUIRED) int size,
        @Schema(example = "42", requiredMode = Schema.RequiredMode.REQUIRED) long totalElements,
        @Schema(example = "3", requiredMode = Schema.RequiredMode.REQUIRED) int totalPages,
        @Schema(example = "true", requiredMode = Schema.RequiredMode.REQUIRED) boolean hasNext
) {

    public static PageInfo from(Page<?> page) {
        return new PageInfo(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }
}
