package com.nexters.palang.domain.passage;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.ReadingStatus;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.decoration.domain.Decoration;
import com.nexters.palang.domain.decoration.domain.EffectType;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// 대목/흔적 조회 플로우를 실제 Spring 컨텍스트 + H2 DB로 end-to-end 검증한다.
// (읽기상태 노출 필터 → 꾸밈 병합 → 흔적 정렬)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PassageReadFlowIntegrationTest {

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

    private Long bookId;
    private Long writerId;
    private Long readerId;
    private Passage spoilerPage1;
    private Passage page3;
    private Passage page6;

    @BeforeEach
    void setUp() {
        Book book = bookRepository.save(Book.builder()
                .title("책").author("작가").publisher("출판사").pageCount(300).build());
        User writer = userRepository.save(User.builder()
                .nickname("작성자").snsProvider(SnsProvider.KAKAO).snsId("writer").termsAgreedAt(LocalDateTime.now()).build());
        User reader = userRepository.save(User.builder()
                .nickname("독자").snsProvider(SnsProvider.KAKAO).snsId("reader").termsAgreedAt(LocalDateTime.now()).build());
        bookId = book.getId();
        writerId = writer.getId();
        readerId = reader.getId();

        spoilerPage1 = passageRepository.save(Passage.builder()
                .book(book).creator(writer).pageNumber(1).quotedText("스포일러 문장").isSpoiler(true)
                .normalizedHash("hash-1").build());
        page3 = passageRepository.save(Passage.builder()
                .book(book).creator(writer).pageNumber(3).quotedText("3페이지 문장").isSpoiler(false)
                .normalizedHash("hash-3").build());
        page6 = passageRepository.save(Passage.builder()
                .book(book).creator(writer).pageNumber(6).quotedText("6페이지 문장").isSpoiler(false)
                .normalizedHash("hash-6").build());
    }

    private Opinion opinion(Passage passage, Long userId, String content, int likeCount) {
        User user = userRepository.findById(userId).orElseThrow();
        Opinion opinion = Opinion.builder().passage(passage).user(user).content(content).build();
        ReflectionTestUtils.setField(opinion, "likeCount", likeCount);
        return opinionRepository.save(opinion);
    }

    @Test
    @DisplayName("비로그인 사용자는 스포일러가 아닌 첫 페이지만 볼 수 있고, 다른 페이지를 요청하면 401이다")
    void anonymousUserSeesOnlyFirstNonSpoilerPage() throws Exception {
        mockMvc.perform(get("/api/books/" + bookId + "/passages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNumbers[0]").value(3))
                .andExpect(jsonPath("$.data.pageNumbers.length()").value(1));

        mockMvc.perform(get("/api/books/" + bookId + "/pages/3/passages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passages[0].passageId").value(page3.getId()));

        mockMvc.perform(get("/api/books/" + bookId + "/pages/6/passages"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("읽는 중(READING) 상태의 로그인 사용자는 현재 페이지까지의 대목을 볼 수 있다")
    void readingUserSeesUpToCurrentPage() throws Exception {
        mockMvc.perform(put("/api/users/me/book-status")
                        .header("X-Debug-User-Id", readerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": " + bookId + ", \"status\": \"READING\", \"currentPage\": 6}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/books/" + bookId + "/passages").header("X-Debug-User-Id", readerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNumbers[0]").value(3))
                .andExpect(jsonPath("$.data.pageNumbers[1]").value(6));

        mockMvc.perform(get("/api/books/" + bookId + "/pages/6/passages").header("X-Debug-User-Id", readerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passages[0].passageId").value(page6.getId()));
    }

    @Test
    @DisplayName("겹치지 않는 꾸밈 중 좋아요 많은 순으로 최대 3개까지만 병합되어 노출된다")
    void mergedDecorationsShowTopThreeNonOverlapping() throws Exception {
        Opinion popular = opinion(page3, readerId, "인기 흔적", 10);
        decoration(popular, 0, 5);
        Opinion overlapping = opinion(page3, readerId, "겹치는 흔적", 20);
        decoration(overlapping, 3, 8);
        Opinion another = opinion(page3, readerId, "또 다른 흔적", 5);
        decoration(another, 10, 15);

        mockMvc.perform(get("/api/books/" + bookId + "/pages/3/passages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passages[0].decorations.length()").value(2))
                .andExpect(jsonPath("$.data.passages[0].decorations[0].startOffset").value(3))
                .andExpect(jsonPath("$.data.passages[0].decorations[1].startOffset").value(10));
    }

    @Test
    @DisplayName("흔적 목록을 좋아요순으로 조회하면 좋아요가 많은 흔적부터 반환된다")
    void getOpinionsSortedByLikes() throws Exception {
        opinion(page3, readerId, "적은 좋아요", 1);
        Opinion popular = opinion(page3, readerId, "많은 좋아요", 100);

        mockMvc.perform(get("/api/passages/" + page3.getId() + "/opinions").param("sortType", "LIKES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opinions[0].opinionId").value(popular.getId()));
    }

    private Decoration decoration(Opinion opinion, int start, int end) {
        Decoration decoration = Decoration.builder()
                .opinion(opinion).startOffset(start).endOffset(end).effectType(EffectType.UNDERLINE).build();
        opinion.addDecoration(decoration);
        return decoration;
    }
}
