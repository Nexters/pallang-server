package com.nexters.palang.domain.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FilterBooksResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<BookOptionResponse> books
) {
}
