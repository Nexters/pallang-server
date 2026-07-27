package com.nexters.palang.domain.book.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.book.application.BookActivityProjection;
import com.nexters.palang.domain.book.application.BookService;
import com.nexters.palang.domain.book.application.ExternalBookResult;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.book.presentation.dto.CreateBookRequest;
import com.nexters.palang.global.security.CurrentUserProvider;
import com.nexters.palang.global.security.LoginRequiredException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

@WebMvcTest(BookController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 20);

    private Book book(Long id, String title) {
        Book book = Book.builder()
                .title(title)
                .author("작가")
                .publisher("출판사")
                .pageCount(300)
                .source(BookSource.MANUAL)
                .build();
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }

    @Test
    @DisplayName("키워드로 도서 외부 검색을 요청하면 결과 목록을 반환한다")
    void searchExternalBooks() throws Exception {
        given(bookService.searchExternalBooks(eq("제목"), any(Pageable.class))).willReturn(
                new PageImpl<>(List.of(new ExternalBookResult("제목", "작가", "출판사", "isbn", "cover")),
                        DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/books/search").param("keyword", "제목"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.books[0].title").value("제목"))
                .andExpect(jsonPath("$.data.books[0].pageCount").doesNotExist())
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1));
    }

    @Test
    @DisplayName("keyword 없이 도서 외부 검색을 요청하면 400 에러가 발생한다")
    void searchExternalBooksFailsWhenKeywordIsMissing() throws Exception {
        mockMvc.perform(get("/api/books/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }

    @Test
    @DisplayName("size에 숫자가 아닌 값을 주면 400 에러가 발생한다")
    void searchExternalBooksFailsWhenSizeIsNotNumber() throws Exception {
        mockMvc.perform(get("/api/books/search").param("keyword", "제목").param("size", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }

    @Test
    @DisplayName("키워드로 도서 내부 검색을 요청하면 등록된 도서 목록을 반환한다")
    void searchInternalBooks() throws Exception {
        given(bookService.searchInternalBooks(eq("제목"), any(Pageable.class))).willReturn(
                new PageImpl<>(List.of(book(1L, "제목")), DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/books/internal-search").param("keyword", "제목"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.books[0].bookId").value(1))
                .andExpect(jsonPath("$.data.books[0].title").value("제목"));
    }

    @Test
    @DisplayName("필수 항목을 채워 도서를 직접 등록하면 등록된 도서를 반환한다")
    void createBook() throws Exception {
        CreateBookRequest request = new CreateBookRequest("제목", "작가", "출판사", 300, "isbn", "cover");
        given(bookService.createBook(any())).willReturn(book(1L, "제목"));

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookId").value(1));
    }

    @Test
    @DisplayName("필수 항목이 비어 있는 상태로 도서 등록을 요청하면 400 에러가 발생한다")
    void createBookFailsWhenRequiredFieldIsBlank() throws Exception {
        CreateBookRequest request = new CreateBookRequest("", "작가", "출판사", 300, null, null);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }

    @Test
    @DisplayName("홈 캐러셀 도서 목록을 요청하면 대목/흔적 수와 함께 반환한다")
    void getHomeCarouselBooks() throws Exception {
        given(bookService.getHomeCarouselBooks(any(Pageable.class))).willReturn(
                new PageImpl<>(List.of(new BookActivityProjection(1L, "제목", "작가", "cover", 3, 7)),
                        DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/home/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.books[0].passageCount").value(3))
                .andExpect(jsonPath("$.data.books[0].opinionCount").value(7));
    }

    @Test
    @DisplayName("내가 최근에 남긴 도서 목록을 요청하면 현재 사용자 기준으로 조회한다")
    void getRecentBooks() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(bookService.getRecentBooks(eq(1L), any(Pageable.class))).willReturn(
                new PageImpl<>(List.of(book(1L, "최근 책")), DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/books/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.books[0].title").value("최근 책"));
    }

    @Test
    @DisplayName("인증 없이 내가 최근에 남긴 도서 목록을 요청하면 401 에러가 발생한다")
    void getRecentBooksFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(get("/api/books/recent"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("인기 도서 목록을 요청하면 흔적 많은 순 목록을 반환한다")
    void getPopularBooks() throws Exception {
        given(bookService.getPopularBooks(any(Pageable.class))).willReturn(
                new PageImpl<>(List.of(new BookActivityProjection(1L, "인기 도서", "작가", "cover", 3, 10)),
                        DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/books/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.books[0].opinionCount").value(10));
    }

    @Test
    @DisplayName("page/size 파라미터를 주면 그 값으로 Pageable을 구성해서 서비스에 전달한다")
    void getPopularBooksUsesGivenPageAndSize() throws Exception {
        given(bookService.getPopularBooks(any(Pageable.class))).willReturn(
                new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        mockMvc.perform(get("/api/books/popular").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookService).getPopularBooks(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }
}
