package com.nexters.palang.domain.passage.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class PassageRequest {

    public record Create(
            @NotNull(message = "도서 ID는 필수입니다.")
            Long bookId,

            @NotNull(message = "작성자 ID는 필수입니다.")
            Long creatorId,

            @Positive(message = "페이지 번호는 1 이상이어야 합니다.")
            int pageNumber,

            @NotBlank(message = "인용 문구는 비어 있을 수 없습니다.")
            @Size(max = 150, message = "인용 문구는 150자를 초과할 수 없습니다.")
            String quotedText,

            boolean isSpoiler
    ) {
    }
}
