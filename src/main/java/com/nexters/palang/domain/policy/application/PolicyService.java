package com.nexters.palang.domain.policy.application;

import com.nexters.palang.domain.policy.common.error.PolicyErrorCode;
import com.nexters.palang.domain.policy.common.error.PolicyException;
import com.nexters.palang.domain.policy.domain.Policy;
import com.nexters.palang.domain.policy.domain.PolicyType;
import com.nexters.palang.domain.policy.infrastructure.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {

    private final PolicyRepository policyRepository;

    public Policy getPolicy(PolicyType type) {
        return policyRepository.findByType(type)
                .orElseThrow(() -> new PolicyException(PolicyErrorCode.POLICY_NOT_FOUND));
    }
}
