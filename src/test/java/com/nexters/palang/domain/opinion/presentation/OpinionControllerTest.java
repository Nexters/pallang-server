package com.nexters.palang.domain.opinion.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.decoration.domain.Decoration;
import com.nexters.palang.domain.decoration.domain.EffectType;
import com.nexters.palang.domain.opinion.application.OpinionLikeResult;
import com.nexters.palang.domain.opinion.application.OpinionLikeService;
import com.nexters.palang.domain.opinion.application.OpinionService;
import com.nexters.palang.domain.opinion.application.OpinionSummaryProjection;
import com.nexters.palang.domain.opinion.common.error.OpinionErrorCode;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.presentation.dto.CreateOpinionRequest;
import com.nexters.palang.domain.opinion.presentation.dto.CreateOpinionRequest.DecorationRequest;
import com.nexters.palang.domain.opinion.presentation.dto.UpdateOpinionRequest;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.security.CurrentUserProvider;
import com.nexters.palang.global.security.LoginRequiredException;
import java.time.LocalDateTime;
import java.util.List;
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

@WebMvcTest(OpinionController.class)
@AutoConfigureMockMvc(addFilters = false)
class OpinionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OpinionService opinionService;

    @MockitoBean
    private OpinionLikeService opinionLikeService;

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

    @Test
    @DisplayName("흔적 목록을 조회하면 정렬된 목록을 반환한다")
    void getOpinions() throws Exception {
        OpinionSummaryProjection projection =
                new OpinionSummaryProjection(1L, 2L, "닉네임", "흔적 내용", 3, LocalDateTime.now(), false, 0L);
        given(opinionService.getOpinions(any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/passages/100/opinions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opinions[0].opinionId").value(1))
                .andExpect(jsonPath("$.data.opinions[0].nickname").value("닉네임"));
    }

    @Test
    @DisplayName("흔적 상세를 조회하면 작성자의 꾸밈을 그대로 반환한다")
    void getOpinion() throws Exception {
        given(opinionService.getOpinion(1L)).willReturn(opinionWithDecoration(1L, 100L));

        mockMvc.perform(get("/api/opinions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opinionId").value(1))
                .andExpect(jsonPath("$.data.decorations[0].startOffset").value(0));
    }

    @Test
    @DisplayName("존재하지 않는 흔적을 상세 조회하면 404 에러가 발생한다")
    void getOpinionFailsWhenNotFound() throws Exception {
        given(opinionService.getOpinion(1L)).willThrow(new OpinionException(OpinionErrorCode.OPINION_NOT_FOUND));

        mockMvc.perform(get("/api/opinions/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("OPINION_404_1"));
    }

    @Test
    @DisplayName("본인이 작성한 흔적을 수정하면 변경된 내용을 반환한다")
    void modifyOpinion() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(opinionService.modifyOpinion(anyLong(), anyLong(), any())).willReturn(opinionWithDecoration(1L, 100L));

        mockMvc.perform(patch("/api/opinions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOpinionRequest("수정된 내용"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opinionId").value(1));
    }

    @Test
    @DisplayName("본인이 아닌 사용자가 흔적을 수정하려 하면 403 에러가 발생한다")
    void modifyOpinionFailsWhenNotOwner() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(opinionService.modifyOpinion(anyLong(), anyLong(), any()))
                .willThrow(new OpinionException(OpinionErrorCode.OPINION_FORBIDDEN));

        mockMvc.perform(patch("/api/opinions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOpinionRequest("수정된 내용"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("OPINION_403_1"));
    }

    @Test
    @DisplayName("본인이 작성한 흔적을 삭제하면 200을 반환한다")
    void removeOpinion() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(delete("/api/opinions/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증 없이 흔적을 삭제하려 하면 401 에러가 발생한다")
    void removeOpinionFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(delete("/api/opinions/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("좋아요를 누르지 않은 흔적에 토글을 요청하면 좋아요가 생성된다")
    void toggleOpinionLikeLikes() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(opinionLikeService.toggleLike(1L, 10L)).willReturn(new OpinionLikeResult(10L, true, 1));

        mockMvc.perform(post("/api/opinions/{opinionId}/like", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opinionId").value(10))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }

    @Test
    @DisplayName("이미 좋아요를 누른 흔적에 토글을 요청하면 좋아요가 취소된다")
    void toggleOpinionLikeUnlikes() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(opinionLikeService.toggleLike(1L, 10L)).willReturn(new OpinionLikeResult(10L, false, 0));

        mockMvc.perform(post("/api/opinions/{opinionId}/like", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }

    @Test
    @DisplayName("인증 없이 좋아요를 시도하면 401 에러가 발생한다")
    void toggleOpinionLikeFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(post("/api/opinions/{opinionId}/like", 10L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("존재하지 않는 흔적에 좋아요를 시도하면 404 에러가 발생한다")
    void toggleOpinionLikeFailsWhenOpinionNotFound() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(opinionLikeService.toggleLike(1L, 999L))
                .willThrow(new OpinionException(OpinionErrorCode.OPINION_NOT_FOUND));

        mockMvc.perform(post("/api/opinions/{opinionId}/like", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("OPINION_404_1"));
    }
}
