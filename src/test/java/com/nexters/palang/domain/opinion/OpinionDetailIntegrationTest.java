package com.nexters.palang.domain.opinion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.decoration.domain.Decoration;
import com.nexters.palang.domain.decoration.domain.EffectType;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.opinion.presentation.dto.UpdateOpinionRequest;
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

// open-in-view: false 환경에서 opinion.getUser()/getDecorations()가 컨트롤러 단 Mapper에서
// lazy-load되어 LazyInitializationException(500)이 나던 흔적 상세/수정 경로를 검증한다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OpinionDetailIntegrationTest {

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
                .book(book).creator(author).pageNumber(1).quotedText("우리는 모두 이야기를 찾아 헤맨다.")
                .isSpoiler(false).normalizedHash("hash").build());
        Opinion opinion = Opinion.builder()
                .passage(passage).user(author).content("흔적 내용").build();
        opinion.addDecoration(Decoration.builder()
                .startOffset(0).endOffset(3).effectType(EffectType.UNDERLINE).color("#PRIMARY").build());
        opinionRepository.save(opinion);

        opinionId = opinion.getId();
        authorId = author.getId();
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

    @Test
    @DisplayName("꾸밈이 있는 흔적을 상세 조회하면 닉네임과 꾸밈 목록이 정상적으로 내려온다")
    void getOpinionReturnsNicknameAndDecorations() throws Exception {
        mockMvc.perform(get("/api/opinions/{opinionId}", opinionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("작성자"))
                .andExpect(jsonPath("$.data.decorations[0].effectType").value("UNDERLINE"));
    }

    @Test
    @DisplayName("꾸밈이 있는 흔적을 수정하면 닉네임과 꾸밈 목록이 정상적으로 내려온다")
    void modifyOpinionReturnsNicknameAndDecorations() throws Exception {
        UpdateOpinionRequest request = new UpdateOpinionRequest("다시 읽어보니 더 와닿는 문장이었어요.");

        mockMvc.perform(patch("/api/opinions/{opinionId}", opinionId)
                        .header("Authorization", bearerToken(authorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("작성자"))
                .andExpect(jsonPath("$.data.content").value("다시 읽어보니 더 와닿는 문장이었어요."))
                .andExpect(jsonPath("$.data.decorations[0].effectType").value("UNDERLINE"));
    }
}
