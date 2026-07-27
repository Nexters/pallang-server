package com.nexters.palang.domain.policy.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexters.palang.domain.policy.application.PolicyService;
import com.nexters.palang.domain.policy.common.error.PolicyErrorCode;
import com.nexters.palang.domain.policy.common.error.PolicyException;
import com.nexters.palang.domain.policy.domain.Policy;
import com.nexters.palang.domain.policy.domain.PolicyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PolicyController.class)
@AutoConfigureMockMvc(addFilters = false)
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PolicyService policyService;

    private Policy policy(PolicyType type, String content) {
        return Policy.builder().type(type).content(content).build();
    }

    @Test
    @DisplayName("이용약관을 요청하면 markdown 콘텐츠를 반환한다")
    void getTermsPolicy() throws Exception {
        given(policyService.getPolicy(eq(PolicyType.TERMS))).willReturn(policy(PolicyType.TERMS, "# 이용약관\n\n내용"));

        mockMvc.perform(get("/api/policies/TERMS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.policyType").value("TERMS"))
                .andExpect(jsonPath("$.data.content").value("# 이용약관\n\n내용"));
    }

    @Test
    @DisplayName("존재하지 않는 약관을 요청하면 404 에러가 발생한다")
    void getPolicyFailsWhenNotFound() throws Exception {
        given(policyService.getPolicy(eq(PolicyType.PRIVACY)))
                .willThrow(new PolicyException(PolicyErrorCode.POLICY_NOT_FOUND));

        mockMvc.perform(get("/api/policies/PRIVACY"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("POLICY_404_1"));
    }
}
