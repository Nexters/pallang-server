package com.nexters.palang.domain.opinion.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.decoration.domain.Decoration;
import com.nexters.palang.domain.decoration.domain.EffectType;
import com.nexters.palang.domain.opinion.application.OpinionService;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.presentation.dto.CreateOpinionRequest;
import com.nexters.palang.domain.opinion.presentation.dto.CreateOpinionRequest.DecorationRequest;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.security.CurrentUserProvider;
import com.nexters.palang.global.security.LoginRequiredException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OpinionController.class)
class OpinionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OpinionService opinionService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private Opinion opinionWithDecoration(Long opinionId, Long passageId) {
        Book book = Book.builder().title("제목").author("작가").publisher("출판사").pageCount(300).build();
        Passage passage = Passage.builder().book(book).build();
        ReflectionTestUtils.setField(passage, "id", passageId);
        Opinion opinion = Opinion.createWithDecorations(passage, User.builder().build(), "흔적 내용",
                List.of(Decoration.builder().startOffset(0).endOffset(5).effectType(EffectType.UNDERLINE).build()));
        ReflectionTestUtils.setField(opinion, "id", opinionId);
        return opinion;
    }

    private CreateOpinionRequest request(Long passageId) {
        return new CreateOpinionRequest(1L, 5, "발췌 문장", false, passageId, "흔적 내용",
                List.of(new DecorationRequest(0, 5, EffectType.UNDERLINE, null)));
    }

    @Test
    @DisplayName("passageId 없이 흔적을 작성하면 새 Passage와 함께 생성된다")
    void createOpinionWithoutPassageId() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(opinionService.createOpinion(anyLong(), any())).willReturn(opinionWithDecoration(1L, 100L));

        mockMvc.perform(post("/api/opinions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opinionId").value(1))
                .andExpect(jsonPath("$.data.merged").value(false))
                .andExpect(jsonPath("$.data.decorations[0].startOffset").value(0));
    }

    @Test
    @DisplayName("passageId를 지정해 흔적을 작성하면 병합 여부가 true로 반환된다")
    void createOpinionWithPassageId() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(opinionService.createOpinion(anyLong(), any())).willReturn(opinionWithDecoration(1L, 100L));

        mockMvc.perform(post("/api/opinions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(100L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merged").value(true));
    }

    @Test
    @DisplayName("인증 없이 흔적을 작성하면 401 에러가 발생한다")
    void createOpinionFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(post("/api/opinions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(null))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("꾸밈 효과 없이 흔적을 작성하면 400 에러가 발생한다")
    void createOpinionFailsWhenDecorationsIsEmpty() throws Exception {
        CreateOpinionRequest request = new CreateOpinionRequest(1L, 5, "발췌 문장", false, null, "흔적 내용", List.of());

        mockMvc.perform(post("/api/opinions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }
}
