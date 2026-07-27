package com.nexters.palang.domain.policy.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.nexters.palang.domain.policy.common.error.PolicyException;
import com.nexters.palang.domain.policy.domain.Policy;
import com.nexters.palang.domain.policy.domain.PolicyType;
import com.nexters.palang.domain.policy.infrastructure.PolicyRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(policyRepository);
    }

    @Test
    @DisplayName("존재하는 약관 종류로 조회하면 해당 약관을 반환한다")
    void getPolicy() {
        Policy policy = Policy.builder().type(PolicyType.TERMS).content("이용약관 내용").build();
        given(policyRepository.findByType(PolicyType.TERMS)).willReturn(Optional.of(policy));

        Policy result = policyService.getPolicy(PolicyType.TERMS);

        assertThat(result).isEqualTo(policy);
    }

    @Test
    @DisplayName("존재하지 않는 약관 종류로 조회하면 예외가 발생한다")
    void getPolicyFailsWhenNotFound() {
        given(policyRepository.findByType(PolicyType.PRIVACY)).willReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.getPolicy(PolicyType.PRIVACY))
                .isInstanceOf(PolicyException.class);
    }
}
