package com.nexters.palang.domain.opinion.presentation.dto;

import com.nexters.palang.domain.opinion.domain.Opinion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOpinionRequest(
        @NotBlank(message = "흔적 내용은 비어 있을 수 없습니다.")
        @Size(max = Opinion.CONTENT_MAX_LENGTH, message = "흔적 내용은 {max}자를 초과할 수 없습니다.")
        @Schema(example = "다시 읽어보니 더 와닿는 문장이었어요.")
        String content
) {
}
