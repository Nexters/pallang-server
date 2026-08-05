package com.nexters.palang.domain.report.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.report.application.ReportService;
import com.nexters.palang.domain.report.common.ReportErrorCode;
import com.nexters.palang.domain.report.common.ReportException;
import com.nexters.palang.domain.report.domain.Report;
import com.nexters.palang.domain.report.domain.ReportReason;
import com.nexters.palang.domain.report.domain.ReportTargetType;
import com.nexters.palang.domain.report.presentation.dto.CreateReportRequest;
import com.nexters.palang.global.security.CurrentUserProvider;
import com.nexters.palang.global.security.LoginRequiredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private Report report(Long id, ReportTargetType targetType, Long targetId) {
        Report built = Report.of(null, targetType, targetId, ReportReason.SPAM, null);
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    @Test
    @DisplayName("흔적을 신고하면 신고 결과를 반환한다")
    void reportOpinion() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(reportService.reportOpinion(eq(1L), eq(1L), any(CreateReportRequest.class)))
                .willReturn(report(1L, ReportTargetType.OPINION, 1L));

        mockMvc.perform(post("/api/opinions/1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateReportRequest(ReportReason.SPAM, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(1))
                .andExpect(jsonPath("$.data.targetType").value("OPINION"));
    }

    @Test
    @DisplayName("인증 없이 흔적을 신고하면 401 에러가 발생한다")
    void reportOpinionFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(post("/api/opinions/1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateReportRequest(ReportReason.SPAM, null))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("본인이 작성한 흔적을 신고하면 400 에러가 발생한다")
    void reportOpinionFailsWhenSelfReport() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(reportService.reportOpinion(eq(1L), eq(1L), any(CreateReportRequest.class)))
                .willThrow(new ReportException(ReportErrorCode.SELF_REPORT_NOT_ALLOWED));

        mockMvc.perform(post("/api/opinions/1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateReportRequest(ReportReason.SPAM, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("REPORT_400_2"));
    }

    @Test
    @DisplayName("사유 없이 흔적을 신고하면 400 에러가 발생한다")
    void reportOpinionFailsWhenReasonMissing() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(post("/api/opinions/1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }

    @Test
    @DisplayName("이미 신고한 흔적을 다시 신고하면 409 에러가 발생한다")
    void reportOpinionFailsWhenDuplicate() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(reportService.reportOpinion(eq(1L), eq(1L), any(CreateReportRequest.class)))
                .willThrow(new ReportException(ReportErrorCode.DUPLICATE_REPORT));

        mockMvc.perform(post("/api/opinions/1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateReportRequest(ReportReason.SPAM, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("REPORT_409_1"));
    }

    @Test
    @DisplayName("댓글을 신고하면 신고 결과를 반환한다")
    void reportComment() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(reportService.reportComment(eq(1L), eq(1L), any(CreateReportRequest.class)))
                .willReturn(report(2L, ReportTargetType.COMMENT, 1L));

        mockMvc.perform(post("/api/comments/1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateReportRequest(ReportReason.ABUSE, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(2))
                .andExpect(jsonPath("$.data.targetType").value("COMMENT"));
    }
}
