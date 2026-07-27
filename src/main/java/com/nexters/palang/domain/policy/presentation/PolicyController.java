package com.nexters.palang.domain.policy.presentation;

import com.nexters.palang.domain.policy.application.PolicyMapper;
import com.nexters.palang.domain.policy.application.PolicyService;
import com.nexters.palang.domain.policy.domain.PolicyType;
import com.nexters.palang.domain.policy.presentation.dto.PolicyResponse;
import com.nexters.palang.global.common.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PolicyController implements PolicyApi {

    private final PolicyService policyService;

    @Override
    @GetMapping("/api/policies/{type}")
    public ResponseEntity<DataResponse<PolicyResponse>> getPolicy(@PathVariable PolicyType type) {
        return ResponseEntity.ok(DataResponse.from(PolicyMapper.toResponse(policyService.getPolicy(type))));
    }
}
