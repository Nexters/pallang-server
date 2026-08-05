package com.nexters.palang.domain.comment.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.comment.application.CommentService;
import com.nexters.palang.domain.comment.application.RootCommentGroup;
import com.nexters.palang.domain.comment.common.CommentErrorCode;
import com.nexters.palang.domain.comment.common.CommentException;
import com.nexters.palang.domain.comment.common.NestedReplyNotAllowedException;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.comment.presentation.dto.CreateCommentRequest;
import com.nexters.palang.domain.comment.presentation.dto.UpdateCommentRequest;
import com.nexters.palang.domain.opinion.common.error.OpinionErrorCode;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.security.CurrentUserProvider;
import com.nexters.palang.global.security.LoginRequiredException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 20);

    private User user(Long id) {
        User built = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private Opinion opinion(Long id) {
        Opinion built = Opinion.builder().user(user(id)).content("흔적").build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private Comment rootComment(Long id, User author) {
        Comment comment = Comment.root(opinion(100L), author, "원댓글");
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    private Comment replyComment(Long id, Comment parent, User author) {
        Comment comment = Comment.reply(parent, author, "답글");
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    @Test
    @DisplayName("댓글 목록을 조회하면 원댓글과 답글 미리보기를 함께 반환한다")
    void getComments() throws Exception {
        User writer = user(1L);
        Comment root = rootComment(1L, writer);
        Comment reply = replyComment(2L, root, writer);
        RootCommentGroup group = new RootCommentGroup(root, List.of(reply), 1);
        given(commentService.getRootComments(eq(1L), any(), any())).willReturn(
                new PageImpl<>(List.of(group), DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/opinions/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comments[0].commentId").value(1))
                .andExpect(jsonPath("$.data.comments[0].replies[0].commentId").value(2))
                .andExpect(jsonPath("$.data.comments[0].replyCount").value(1))
                .andExpect(jsonPath("$.data.comments[0].hasMoreReplies").value(false));
    }

    @Test
    @DisplayName("존재하지 않는 흔적의 댓글 목록을 조회하면 404 에러가 발생한다")
    void getCommentsFailsWhenOpinionNotFound() throws Exception {
        given(commentService.getRootComments(eq(999L), any(), any()))
                .willThrow(new OpinionException(OpinionErrorCode.OPINION_NOT_FOUND));

        mockMvc.perform(get("/api/opinions/999/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("OPINION_404_1"));
    }

    @Test
    @DisplayName("로그인 상태로 댓글 목록을 조회하면 차단 필터링을 위해 currentUserId를 서비스에 전달한다")
    void getCommentsPassesCurrentUserIdWhenAuthenticated() throws Exception {
        given(currentUserProvider.findCurrentUserId()).willReturn(java.util.Optional.of(1L));
        given(commentService.getRootComments(eq(1L), any(), eq(1L))).willReturn(
                new PageImpl<>(List.of(), DEFAULT_PAGEABLE, 0));

        mockMvc.perform(get("/api/opinions/1/comments"))
                .andExpect(status().isOk());

        verify(commentService).getRootComments(eq(1L), any(), eq(1L));
    }

    @Test
    @DisplayName("답글 더보기를 요청하면 답글 목록을 반환한다")
    void getReplies() throws Exception {
        User writer = user(1L);
        Comment root = rootComment(1L, writer);
        Comment reply = replyComment(2L, root, writer);
        given(commentService.getReplies(eq(1L), any(), any())).willReturn(
                new PageImpl<>(List.of(reply), DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/comments/1/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comments[0].commentId").value(2));
    }

    @Test
    @DisplayName("존재하지 않는 원댓글의 답글을 조회하면 404 에러가 발생한다")
    void getRepliesFailsWhenParentNotFound() throws Exception {
        given(commentService.getReplies(eq(999L), any(), any()))
                .willThrow(new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));

        mockMvc.perform(get("/api/comments/999/replies"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("COMMENT_404_1"));
    }

    @Test
    @DisplayName("댓글을 작성하면 작성된 댓글을 반환한다")
    void createComment() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        Comment created = rootComment(1L, user(1L));
        given(commentService.createComment(eq(1L), eq(1L), any(CreateCommentRequest.class))).willReturn(created);

        mockMvc.perform(post("/api/opinions/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest(null, "댓글 내용"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentId").value(1));
    }

    @Test
    @DisplayName("인증 없이 댓글을 작성하면 401 에러가 발생한다")
    void createCommentFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(post("/api/opinions/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest(null, "댓글 내용"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("빈 내용으로 댓글을 작성하면 400 에러가 발생한다")
    void createCommentFailsWhenContentIsBlank() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(post("/api/opinions/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest(null, " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }

    @Test
    @DisplayName("답글에 다시 답글을 작성하면 400 에러가 발생한다")
    void createCommentFailsWhenNestedReply() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(commentService.createComment(eq(1L), eq(1L), any(CreateCommentRequest.class)))
                .willThrow(new NestedReplyNotAllowedException());

        mockMvc.perform(post("/api/opinions/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest(2L, "답글의 답글"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMENT_400_1"));
    }

    @Test
    @DisplayName("댓글을 수정하면 수정된 내용을 반환한다")
    void modifyComment() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        Comment modified = rootComment(1L, user(1L));
        modified.updateContent("수정된 내용");
        given(commentService.modifyComment(eq(1L), eq(1L), any(UpdateCommentRequest.class))).willReturn(modified);

        mockMvc.perform(patch("/api/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCommentRequest("수정된 내용"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("수정된 내용"));
    }

    @Test
    @DisplayName("본인이 작성하지 않은 댓글을 수정하면 403 에러가 발생한다")
    void modifyCommentFailsWhenNotOwner() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(commentService.modifyComment(eq(1L), eq(1L), any(UpdateCommentRequest.class)))
                .willThrow(new CommentException(CommentErrorCode.COMMENT_FORBIDDEN));

        mockMvc.perform(patch("/api/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCommentRequest("수정 시도"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("COMMENT_403_1"));
    }

    @Test
    @DisplayName("댓글을 삭제하면 서비스에 삭제를 위임한다")
    void removeComment() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(delete("/api/comments/1"))
                .andExpect(status().isOk());

        verify(commentService).removeComment(1L, 1L);
    }

    @Test
    @DisplayName("본인이 작성하지 않은 댓글을 삭제하면 403 에러가 발생한다")
    void removeCommentFailsWhenNotOwner() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        doThrow(new CommentException(CommentErrorCode.COMMENT_FORBIDDEN))
                .when(commentService).removeComment(1L, 1L);

        mockMvc.perform(delete("/api/comments/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("COMMENT_403_1"));
    }
}
