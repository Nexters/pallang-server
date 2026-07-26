package com.nexters.palang.domain.user.presentation.dto;

import com.nexters.palang.domain.user.domain.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBackgroundColorRequest(
        @NotBlank(message = "배경색은 필수입니다.")
        @Size(max = User.BACKGROUND_COLOR_MAX_LENGTH, message = "배경색은 20자를 초과할 수 없습니다.")
        String backgroundColor
) {
}
