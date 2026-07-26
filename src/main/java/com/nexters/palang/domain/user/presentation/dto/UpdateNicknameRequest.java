package com.nexters.palang.domain.user.presentation.dto;

import com.nexters.palang.domain.user.domain.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = User.NICKNAME_MAX_LENGTH, message = "닉네임은 15자를 초과할 수 없습니다.")
        String nickname
) {
}
