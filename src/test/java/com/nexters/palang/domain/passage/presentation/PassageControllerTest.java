package com.nexters.palang.domain.passage.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.passage.application.PassageOcrService;
import com.nexters.palang.domain.passage.application.SimilarPassageFinder;
import com.nexters.palang.domain.passage.application.SimilarPassageProjection;
import com.nexters.palang.domain.passage.presentation.request.PassageRequest;
import com.nexters.palang.global.security.CurrentUserProvider;
import com.nexters.palang.global.security.LoginRequiredException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PassageController.class)
class PassageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PassageOcrService passageOcrService;

    @MockitoBean
    private SimilarPassageFinder similarPassageFinder;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    @DisplayName("유사 문장 후보를 조회하면 대목 목록을 반환한다")
    void checkSimilarPassages() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(similarPassageFinder.find(any(), anyInt(), anyString()))
                .willReturn(List.of(new SimilarPassageProjection(10L, "발췌 문장", 5, 2L)));

        PassageRequest.SimilarCheck request = new PassageRequest.SimilarCheck(1L, 5, "발췌 문장");

        mockMvc.perform(post("/api/passages/similar-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passages[0].passageId").value(10))
                .andExpect(jsonPath("$.data.passages[0].opinionCount").value(2));
    }

    @Test
    @DisplayName("유사 문장 후보가 없으면 빈 배열을 반환한다")
    void checkSimilarPassagesReturnsEmptyArrayWhenNoCandidates() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(similarPassageFinder.find(any(), anyInt(), anyString())).willReturn(List.of());

        PassageRequest.SimilarCheck request = new PassageRequest.SimilarCheck(1L, 5, "발췌 문장");

        mockMvc.perform(post("/api/passages/similar-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passages").isEmpty());
    }

    @Test
    @DisplayName("인증 없이 유사 문장 후보를 조회하면 401 에러가 발생한다")
    void checkSimilarPassagesFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        PassageRequest.SimilarCheck request = new PassageRequest.SimilarCheck(1L, 5, "발췌 문장");

        mockMvc.perform(post("/api/passages/similar-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("인용 문구 없이 유사 문장 후보를 조회하면 400 에러가 발생한다")
    void checkSimilarPassagesFailsWhenQuotedTextIsBlank() throws Exception {
        PassageRequest.SimilarCheck request = new PassageRequest.SimilarCheck(1L, 5, "");

        mockMvc.perform(post("/api/passages/similar-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }
}
