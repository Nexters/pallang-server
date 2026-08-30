package com.nexters.palang.domain.admin.presentation.dto;

import com.nexters.palang.domain.opinion.domain.Opinion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUpdateOpinionRequest(
        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = Opinion.CONTENT_MAX_LENGTH, message = "내용은 최대 500자까지 가능합니다.")
        @Schema(example = "수정된 의견 내용") String content
) {
}
