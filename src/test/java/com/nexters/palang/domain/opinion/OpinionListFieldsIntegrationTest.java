package com.nexters.palang.domain.opinion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.comment.infrastructure.CommentRepository;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.domain.OpinionLike;
import com.nexters.palang.domain.opinion.infrastructure.OpinionLikeRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// 흔적 목록 응답에 추가된 liked(로그인 사용자 기준, 비로그인은 false)와
// commentCount(답글 포함, 삭제 댓글 제외)를 검증한다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OpinionListFieldsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private OpinionRepository opinionRepository;

    @Autowired
    private OpinionLikeRepository opinionLikeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Long passageId;
    private Long opinionId;
    private Long viewerId;

    @BeforeEach
    void setUp() {
        Book book = bookRepository.save(Book.builder()
                .title("프랑켄슈타인").author("메리 셸리").publisher("문학동네").pageCount(300).build());
        User author = userRepository.save(User.builder()
                .nickname("작성자").snsProvider(SnsProvider.KAKAO).snsId("author-1")
                .termsAgreedAt(LocalDateTime.now()).build());
        User viewer = userRepository.save(User.builder()
                .nickname("열람자").snsProvider(SnsProvider.KAKAO).snsId("viewer-1")
                .termsAgreedAt(LocalDateTime.now()).build());
        Passage passage = passageRepository.save(Passage.builder()
                .book(book).creator(author).pageNumber(1).quotedText("문장")
                .isSpoiler(false).normalizedHash("hash").build());
        Opinion opinion = opinionRepository.save(Opinion.builder()
                .passage(passage).user(author).content("흔적 내용").build());
        opinionLikeRepository.save(OpinionLike.builder().user(viewer).opinion(opinion).build());

        Comment root = commentRepository.save(Comment.root(opinion, author, "원댓글"));
        commentRepository.save(Comment.reply(root, author, "답글"));
        Comment deletedRoot = commentRepository.save(Comment.root(opinion, author, "삭제될 댓글"));
        deletedRoot.delete();

        passageId = passage.getId();
        opinionId = opinion.getId();
        viewerId = viewer.getId();
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

    @Test
    @DisplayName("좋아요를 누른 로그인 사용자에게는 liked=true, commentCount는 답글 포함/삭제 제외로 내려온다")
    void loggedInUserSeesLikedTrueAndCorrectCommentCount() throws Exception {
        mockMvc.perform(get("/api/passages/{passageId}/opinions", passageId)
                        .header("Authorization", bearerToken(viewerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opinions[0].opinionId").value(opinionId))
                .andExpect(jsonPath("$.data.opinions[0].liked").value(true))
                .andExpect(jsonPath("$.data.opinions[0].commentCount").value(2));
    }

    @Test
    @DisplayName("비로그인 사용자는 liked=false로 내려온다")
    void anonymousUserSeesLikedFalse() throws Exception {
        mockMvc.perform(get("/api/passages/{passageId}/opinions", passageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opinions[0].liked").value(false))
                .andExpect(jsonPath("$.data.opinions[0].commentCount").value(2));
    }
}
