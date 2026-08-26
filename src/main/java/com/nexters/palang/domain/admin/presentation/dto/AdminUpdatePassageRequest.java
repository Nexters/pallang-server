package com.nexters.palang.domain.admin.presentation.dto;

import com.nexters.palang.domain.passage.domain.Passage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminUpdatePassageRequest(
        @NotBlank(message = "인용문은 필수입니다.")
        @Size(max = Passage.QUOTED_TEXT_MAX_LENGTH, message = "인용문은 최대 150자까지 가능합니다.")
        @Schema(example = "수정된 인용문") String quotedText,

        @Positive(message = "페이지 번호는 1 이상이어야 합니다.")
        @Schema(example = "42") int pageNumber,

        @Schema(example = "false") boolean isSpoiler
) {
}
