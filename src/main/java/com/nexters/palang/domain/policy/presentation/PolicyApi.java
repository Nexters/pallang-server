package com.nexters.palang.domain.policy.presentation;

import com.nexters.palang.domain.policy.domain.PolicyType;
import com.nexters.palang.domain.policy.presentation.dto.PolicyResponse;
import com.nexters.palang.global.common.error.ErrorResponse;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Policy", description = "약관/정책 API")
public interface PolicyApi {

    @Operation(summary = "약관 조회", description = "이용약관 또는 개인정보처리방침을 markdown 형태로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "type이 TERMS/PRIVACY가 아님 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 약관을 찾을 수 없음 (POLICY_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DataResponse<PolicyResponse>> getPolicy(
            @Parameter(description = "약관 종류 (TERMS: 이용약관, PRIVACY: 개인정보처리방침)", required = true) PolicyType type
    );
}
