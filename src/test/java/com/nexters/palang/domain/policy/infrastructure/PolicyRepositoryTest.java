package com.nexters.palang.domain.policy.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.policy.domain.Policy;
import com.nexters.palang.domain.policy.domain.PolicyType;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class PolicyRepositoryTest {

    @Autowired
    private PolicyRepository policyRepository;

    @Test
    @DisplayName("등록된 약관 종류로 조회하면 해당 약관을 반환한다")
    void findByType() {
        policyRepository.save(Policy.builder().type(PolicyType.TERMS).content("이용약관 내용").build());

        Optional<Policy> result = policyRepository.findByType(PolicyType.TERMS);

        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("이용약관 내용");
    }

    @Test
    @DisplayName("등록되지 않은 약관 종류로 조회하면 빈 값을 반환한다")
    void findByTypeReturnsEmptyWhenNotFound() {
        Optional<Policy> result = policyRepository.findByType(PolicyType.PRIVACY);

        assertThat(result).isEmpty();
    }
}
