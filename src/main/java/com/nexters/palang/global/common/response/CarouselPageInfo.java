package com.nexters.palang.global.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CarouselPageInfo(
        @Schema(example = "40", requiredMode = Schema.RequiredMode.REQUIRED) long offset,
        @Schema(example = "20", requiredMode = Schema.RequiredMode.REQUIRED) int size,
        @Schema(example = "100", requiredMode = Schema.RequiredMode.REQUIRED) long totalElements,
        @Schema(example = "true", requiredMode = Schema.RequiredMode.REQUIRED) boolean hasPrevious,
        @Schema(example = "true", requiredMode = Schema.RequiredMode.REQUIRED) boolean hasNext
) {

    public static CarouselPageInfo of(long offset, int size, int contentSize, long totalElements) {
        boolean hasPrevious = offset > 0;
        boolean hasNext = offset + contentSize < totalElements;
        return new CarouselPageInfo(offset, size, totalElements, hasPrevious, hasNext);
    }
}
