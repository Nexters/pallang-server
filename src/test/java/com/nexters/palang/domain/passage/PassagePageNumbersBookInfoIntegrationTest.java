package com.nexters.palang.domain.passage;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
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

// 대목 페이지 목록(GET /api/books/{bookId}/passages) 응답에 헤더용 책 정보(bookTitle/coverImageUrl)가
// 포함되는지, 존재하지 않는 책이면 404가 나는지 검증한다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PassagePageNumbersBookInfoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PassageRepository passageRepository;

    private Long bookId;

    @BeforeEach
    void setUp() {
        Book book = bookRepository.save(Book.builder()
                .title("프랑켄슈타인").author("메리 셸리").publisher("문학동네").pageCount(300)
                .coverImageUrl("https://example.com/cover.jpg").build());
        User creator = userRepository.save(User.builder()
                .nickname("작성자").snsProvider(SnsProvider.KAKAO).snsId("author-1")
                .termsAgreedAt(LocalDateTime.now()).build());
        passageRepository.save(Passage.builder()
                .book(book).creator(creator).pageNumber(87).quotedText("우리는 모두 이야기를 찾아 헤맨다.")
                .isSpoiler(false).normalizedHash("hash").build());

        bookId = book.getId();
    }

    @Test
    @DisplayName("대목 페이지 목록 조회 응답에 책 제목/커버 이미지가 포함된다")
    void getPageNumbersReturnsBookInfo() throws Exception {
        mockMvc.perform(get("/api/books/{bookId}/passages", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookTitle").value("프랑켄슈타인"))
                .andExpect(jsonPath("$.data.coverImageUrl").value("https://example.com/cover.jpg"))
                .andExpect(jsonPath("$.data.pageNumbers[0]").value(87));
    }

    @Test
    @DisplayName("존재하지 않는 책이면 404 에러가 발생한다")
    void getPageNumbersFailsWhenBookNotFound() throws Exception {
        mockMvc.perform(get("/api/books/{bookId}/passages", 999999L))
                .andExpect(status().isNotFound());
    }
}
