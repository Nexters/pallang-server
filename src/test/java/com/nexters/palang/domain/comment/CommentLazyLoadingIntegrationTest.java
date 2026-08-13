package com.nexters.palang.domain.comment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.comment.infrastructure.CommentRepository;
import com.nexters.palang.domain.comment.presentation.dto.CreateCommentRequest;
import com.nexters.palang.domain.comment.presentation.dto.UpdateCommentRequest;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import com.nexters.palang.global.security.jwt.JwtTokenProvider;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// open-in-view: false 환경에서 컨트롤러 단 Mapper가 lazy user를 참조해 LazyInitializationException(500)이
// 나던 댓글 목록/답글/작성/수정 경로를, 실제 트랜잭션 경계를 넘는 end-to-end 테스트로 검증한다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentLazyLoadingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private OpinionRepository opinionRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Long opinionId;
    private Long authorId;

    @BeforeEach
    void setUp() {
        Book book = bookRepository.save(Book.builder()
                .title("프랑켄슈타인").author("메리 셸리").publisher("문학동네").pageCount(300).build());
        User author = userRepository.save(User.builder()
                .nickname("작성자").snsProvider(SnsProvider.KAKAO).snsId("author-1")
                .termsAgreedAt(LocalDateTime.now()).build());
        Passage passage = passageRepository.save(Passage.builder()
                .book(book).creator(author).pageNumber(1).quotedText("문장")
                .isSpoiler(false).normalizedHash("hash").build());
        Opinion opinion = opinionRepository.save(Opinion.builder()
                .passage(passage).user(author).content("흔적 내용").build());

        opinionId = opinion.getId();
        authorId = author.getId();
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

    @Test
    @DisplayName("댓글을 작성하면 닉네임이 포함된 응답이 정상적으로 내려온다")
    void createCommentReturnsAuthorNickname() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest(null, "저도 같은 생각이에요!");

        mockMvc.perform(post("/api/opinions/{opinionId}/comments", opinionId)
                        .header("Authorization", bearerToken(authorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("작성자"))
                .andExpect(jsonPath("$.data.content").value("저도 같은 생각이에요!"));
    }

    @Test
    @DisplayName("원댓글과 답글이 있을 때 목록 조회 응답에 각각의 닉네임이 정상적으로 내려온다")
    void getCommentsReturnsNicknamesForRootsAndReplies() throws Exception {
        Comment root = commentRepository.save(
                Comment.root(opinionRepository.findById(opinionId).orElseThrow(),
                        userRepository.findById(authorId).orElseThrow(), "원댓글"));
        commentRepository.save(Comment.reply(root, userRepository.findById(authorId).orElseThrow(), "답글"));

        mockMvc.perform(get("/api/opinions/{opinionId}/comments", opinionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comments[0].nickname").value("작성자"))
                .andExpect(jsonPath("$.data.comments[0].replies[0].nickname").value("작성자"));

        mockMvc.perform(get("/api/comments/{commentId}/replies", root.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comments[0].nickname").value("작성자"));
    }

    @Test
    @DisplayName("댓글을 수정하면 닉네임이 포함된 응답이 정상적으로 내려온다")
    void modifyCommentReturnsAuthorNickname() throws Exception {
        Comment root = commentRepository.save(
                Comment.root(opinionRepository.findById(opinionId).orElseThrow(),
                        userRepository.findById(authorId).orElseThrow(), "원댓글"));
        UpdateCommentRequest request = new UpdateCommentRequest("다시 읽어보니 더 공감돼요.");

        mockMvc.perform(patch("/api/comments/{commentId}", root.getId())
                        .header("Authorization", bearerToken(authorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("작성자"))
                .andExpect(jsonPath("$.data.content").value("다시 읽어보니 더 공감돼요."));
    }
}
