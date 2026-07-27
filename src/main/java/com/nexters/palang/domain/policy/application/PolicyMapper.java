package com.nexters.palang.domain.policy.application;

import com.nexters.palang.domain.policy.domain.Policy;
import com.nexters.palang.domain.policy.presentation.dto.PolicyResponse;

public final class PolicyMapper {

    private PolicyMapper() {
    }

    public static PolicyResponse toResponse(Policy policy) {
        return new PolicyResponse(policy.getType(), policy.getContent(), policy.getUpdatedAt());
    }
}
