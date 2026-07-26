package com.nexters.palang.domain.passage.presentation.request;

import com.nexters.palang.domain.passage.domain.Passage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class PassageRequest {

    public record SimilarCheck(
            @NotNull(message = "도서 ID는 필수입니다.")
            @Schema(example = "1")
            Long bookId,

            @Positive(message = "페이지 번호는 1 이상이어야 합니다.")
            @Schema(example = "87")
            int pageNumber,

            @NotBlank(message = "인용 문구는 비어 있을 수 없습니다.")
            @Size(max = Passage.QUOTED_TEXT_MAX_LENGTH, message = "인용 문구는 {max}자를 초과할 수 없습니다.")
            @Schema(example = "우리는 모두 이야기를 찾아 헤맨다.")
            String quotedText
    ) {
    }
}
