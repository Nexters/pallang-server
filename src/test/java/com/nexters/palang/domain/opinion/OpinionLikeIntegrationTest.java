package com.nexters.palang.domain.opinion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// 좋아요 토글이 OpinionLikeService를 거쳐 opinions.like_count를 실제로 동기화하는지
// end-to-end로 검증한다 (backend-plan.md §5.4).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OpinionLikeIntegrationTest {

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

    private Long opinionId;
    private Long likerId;
    private Long anotherLikerId;

    @BeforeEach
    void setUp() {
        Book book = bookRepository.save(Book.builder()
                .title("프랑켄슈타인").author("메리 셸리").publisher("문학동네").pageCount(300).build());
        User author = userRepository.save(User.builder()
                .nickname("작성자").snsProvider(SnsProvider.KAKAO).snsId("author-1")
                .termsAgreedAt(LocalDateTime.now()).build());
        User liker = userRepository.save(User.builder()
                .nickname("좋아요러").snsProvider(SnsProvider.KAKAO).snsId("liker-1")
                .termsAgreedAt(LocalDateTime.now()).build());
        User anotherLiker = userRepository.save(User.builder()
                .nickname("좋아요러2").snsProvider(SnsProvider.KAKAO).snsId("liker-2")
                .termsAgreedAt(LocalDateTime.now()).build());
        Passage passage = passageRepository.save(Passage.builder()
                .book(book).creator(author).pageNumber(1).quotedText("문장")
                .isSpoiler(false).normalizedHash("hash").build());
        Opinion opinion = opinionRepository.save(Opinion.builder()
                .passage(passage).user(author).content("흔적 내용").build());

        opinionId = opinion.getId();
        likerId = liker.getId();
        anotherLikerId = anotherLiker.getId();
    }

    @Test
    @DisplayName("좋아요를 누르면 likeCount가 1 증가하고, 다시 누르면 취소되며 0으로 돌아온다")
    void toggleLikeSyncsLikeCount() throws Exception {
        mockMvc.perform(post("/api/opinions/{opinionId}/like", opinionId)
                        .header("X-Debug-User-Id", likerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        assertThat(opinionRepository.findById(opinionId).orElseThrow().getLikeCount()).isEqualTo(1);

        mockMvc.perform(post("/api/opinions/{opinionId}/like", opinionId)
                        .header("X-Debug-User-Id", likerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));

        assertThat(opinionRepository.findById(opinionId).orElseThrow().getLikeCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("서로 다른 두 사용자가 좋아요를 누르면 likeCount가 각각 반영되어 2가 된다")
    void multipleUsersLikeIncrementsLikeCountIndependently() throws Exception {
        mockMvc.perform(post("/api/opinions/{opinionId}/like", opinionId)
                        .header("X-Debug-User-Id", likerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1));

        mockMvc.perform(post("/api/opinions/{opinionId}/like", opinionId)
                        .header("X-Debug-User-Id", anotherLikerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(2));

        assertThat(opinionRepository.findById(opinionId).orElseThrow().getLikeCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("존재하지 않는 흔적에 좋아요를 시도하면 404 에러가 발생한다")
    void toggleLikeFailsWhenOpinionNotFound() throws Exception {
        mockMvc.perform(post("/api/opinions/{opinionId}/like", 999999L)
                        .header("X-Debug-User-Id", likerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("OPINION_404_1"));
    }

    @Test
    @DisplayName("인증 없이 좋아요를 시도하면 401 에러가 발생한다")
    void toggleLikeFailsWhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/opinions/{opinionId}/like", opinionId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }
}
