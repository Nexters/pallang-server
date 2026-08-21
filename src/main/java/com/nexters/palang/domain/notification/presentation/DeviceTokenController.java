package com.nexters.palang.domain.notification.presentation;

import com.nexters.palang.domain.notification.application.DeviceTokenService;
import com.nexters.palang.domain.notification.presentation.dto.RegisterDeviceTokenRequest;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DeviceTokenController implements DeviceTokenApi {

    private final DeviceTokenService deviceTokenService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @PutMapping("/api/notifications/device-tokens")
    public ResponseEntity<DataResponse<Void>> registerDeviceToken(@RequestBody @Valid RegisterDeviceTokenRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        deviceTokenService.registerOrRefresh(currentUserId, request.token(), request.platform());
        return ResponseEntity.ok(DataResponse.from(null));
    }

    @Override
    @DeleteMapping("/api/notifications/device-tokens")
    public ResponseEntity<DataResponse<Void>> removeDeviceToken(@RequestParam String token) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        deviceTokenService.remove(currentUserId, token);
        return ResponseEntity.ok(DataResponse.from(null));
    }
}
