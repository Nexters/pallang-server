package com.nexters.palang.domain.opinion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// 흔적 작성(직접 입력) 플로우를 실제 Spring 컨텍스트 + H2 DB로 end-to-end 검증한다.
// (유사 문장 조회 → 신규 Passage 생성 → 병합 → 꾸밈 겹침 거부 → 읽기상태 설정)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OpinionWriteFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long bookId;
    private Long userId;

    @BeforeEach
    void setUp() {
        Book book = bookRepository.save(Book.builder()
                .title("프랑켄슈타인").author("메리 셸리").publisher("문학동네").pageCount(300).build());
        User user = userRepository.save(User.builder()
                .nickname("테스터").snsProvider(SnsProvider.KAKAO).snsId("tester-1")
                .termsAgreedAt(LocalDateTime.now()).build());
        bookId = book.getId();
        userId = user.getId();
    }

    @Test
    @DisplayName("직접 입력으로 흔적을 작성하면 새 Passage가 생성되고, 인접 페이지에서 유사 문장으로 조회되며, 병합 흔적을 남길 수 있고, 겹치는 꾸밈은 거부된다")
    void directEntryWriteFlow() throws Exception {
        // 1. passageId 없이 흔적 작성 → 새 Passage 생성
        String createBody = """
                {"bookId": %d, "pageNumber": 10, "quotedText": "나는 오늘도 걷는다", "isSpoiler": false,
                 "passageId": null, "content": "이 문장이 마음에 든다",
                 "decorations": [{"startOffset": 0, "endOffset": 5, "effectType": "UNDERLINE", "color": null}]}
                """.formatted(bookId);

        String createResponse = mockMvc.perform(post("/api/opinions")
                        .header("X-Debug-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merged").value(false))
                .andReturn().getResponse().getContentAsString();
        Long passageId = objectMapper.readTree(createResponse).path("data").path("passageId").asLong();

        // 2. 인접 페이지(11)에서 공백/구두점만 다른 문장으로 유사-체크 → 방금 만든 Passage가 후보로 나와야 한다
        String similarCheckBody = """
                {"bookId": %d, "pageNumber": 11, "quotedText": "나는, 오늘도 걷는다!"}
                """.formatted(bookId);

        mockMvc.perform(post("/api/passages/similar-check")
                        .header("X-Debug-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(similarCheckBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passages[0].passageId").value(passageId));

        // 3. 반환된 passageId로 병합 흔적 작성
        String mergeBody = """
                {"bookId": %d, "pageNumber": 11, "quotedText": "나는, 오늘도 걷는다!", "isSpoiler": false,
                 "passageId": %d, "content": "나도 공감한다",
                 "decorations": [{"startOffset": 0, "endOffset": 3, "effectType": "WAVY", "color": null}]}
                """.formatted(bookId, passageId);

        mockMvc.perform(post("/api/opinions")
                        .header("X-Debug-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mergeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merged").value(true))
                .andExpect(jsonPath("$.data.passageId").value(passageId));

        // 4. 겹치는 꾸밈 효과로 흔적을 작성하면 400
        String overlappingBody = """
                {"bookId": %d, "pageNumber": 12, "quotedText": "완전히 다른 문장입니다", "isSpoiler": false,
                 "passageId": null, "content": "겹치는 꾸밈 테스트",
                 "decorations": [
                   {"startOffset": 0, "endOffset": 10, "effectType": "UNDERLINE", "color": null},
                   {"startOffset": 5, "endOffset": 15, "effectType": "HIGHLIGHT", "color": null}
                 ]}
                """.formatted(bookId);

        mockMvc.perform(post("/api/opinions")
                        .header("X-Debug-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlappingBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("DECORATION_400_2"));

        // 5. 읽기상태 설정 (upsert 확인을 위해 두 번 호출)
        String bookStatusBody = """
                {"bookId": %d, "status": "READING", "currentPage": 50}
                """.formatted(bookId);

        mockMvc.perform(put("/api/users/me/book-status")
                        .header("X-Debug-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookStatusBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentPage").value(50));

        String bookStatusOverflowBody = """
                {"bookId": %d, "status": "READING", "currentPage": 9999}
                """.formatted(bookId);

        mockMvc.perform(put("/api/users/me/book-status")
                        .header("X-Debug-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookStatusOverflowBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("BOOK_400_2"));
    }
}
