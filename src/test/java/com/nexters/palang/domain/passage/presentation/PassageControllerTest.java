package com.nexters.palang.domain.passage.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.passage.application.PassageOcrService;
import com.nexters.palang.domain.passage.application.PassageService;
import com.nexters.palang.domain.passage.application.SimilarPassageFinder;
import com.nexters.palang.domain.passage.application.SimilarPassageProjection;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.presentation.request.PassageRequest;
import com.nexters.palang.global.security.CurrentUserProvider;
import com.nexters.palang.global.security.LoginRequiredException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PassageController.class)
@AutoConfigureMockMvc(addFilters = false)
class PassageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PassageOcrService passageOcrService;

    @MockitoBean
    private SimilarPassageFinder similarPassageFinder;

    @MockitoBean
    private PassageService passageService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private Passage passage(Long id, int pageNumber) {
        Passage passage = Passage.builder().pageNumber(pageNumber).quotedText("발췌 문장").isSpoiler(false).build();
        ReflectionTestUtils.setField(passage, "id", id);
        return passage;
    }

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

    @Test
    @DisplayName("페이지 번호 목록을 조회하면 오름차순으로 반환한다")
    void getPageNumbers() throws Exception {
        given(passageService.getPageNumbers(any(), any()))
                .willReturn(new PageImpl<>(List.of(2, 5), PageRequest.of(0, 20), 2));

        mockMvc.perform(get("/api/books/1/passages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNumbers[0]").value(2))
                .andExpect(jsonPath("$.data.pageNumbers[1]").value(5));
    }

    @Test
    @DisplayName("비로그인 사용자로 페이지 번호를 조회해도 401 없이 조회된다 (soft auth)")
    void getPageNumbersDoesNotRequireAuthentication() throws Exception {
        given(passageService.getPageNumbers(any(), any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/books/1/passages"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("특정 페이지의 대목과 병합된 꾸밈을 함께 반환한다")
    void getPassagesByPage() throws Exception {
        Passage passage = passage(10L, 3);
        given(passageService.getPassagesByPage(any(), anyInt())).willReturn(List.of(passage));
        given(passageService.getMergedDecorationsByPassageId(any())).willReturn(Map.of(10L, List.of()));

        mockMvc.perform(get("/api/books/1/pages/3/passages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passages[0].passageId").value(10))
                .andExpect(jsonPath("$.data.passages[0].pageNumber").value(3));
    }

    @Test
    @DisplayName("비로그인 사용자도 아무 페이지나 조회할 수 있다 (soft auth)")
    void getPassagesByPageDoesNotRequireAuthentication() throws Exception {
        Passage passage = passage(10L, 5);
        given(passageService.getPassagesByPage(any(), anyInt())).willReturn(List.of(passage));
        given(passageService.getMergedDecorationsByPassageId(any())).willReturn(Map.of(10L, List.of()));

        mockMvc.perform(get("/api/books/1/pages/5/passages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passages[0].passageId").value(10));
    }
}
