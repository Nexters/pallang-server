package com.nexters.palang.domain.admin.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        @NotBlank(message = "아이디는 필수입니다.") @Schema(example = "pallang") String username,
        @NotBlank(message = "비밀번호는 필수입니다.") @Schema(example = "********") String password
) {
}
