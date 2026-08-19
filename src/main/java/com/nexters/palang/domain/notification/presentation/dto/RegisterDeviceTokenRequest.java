package com.nexters.palang.domain.notification.presentation.dto;

import com.nexters.palang.domain.notification.domain.DevicePlatform;
import com.nexters.palang.domain.notification.domain.DeviceToken;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDeviceTokenRequest(
        @NotBlank(message = "디바이스 토큰은 필수입니다.")
        @Size(max = DeviceToken.TOKEN_MAX_LENGTH, message = "디바이스 토큰이 너무 깁니다.")
        @Schema(example = "fcm-device-token-example", requiredMode = Schema.RequiredMode.REQUIRED)
        String token,
        @NotNull(message = "플랫폼은 필수입니다.")
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        DevicePlatform platform
) {
}
