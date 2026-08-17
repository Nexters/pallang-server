package com.nexters.palang.domain.book.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.book.application.BookActivityProjection;
import com.nexters.palang.domain.book.application.BookCarouselPage;
import com.nexters.palang.domain.book.application.BookDetail;
import com.nexters.palang.domain.book.application.BookDetailProjection;
import com.nexters.palang.domain.book.application.BookSearchProjection;
import com.nexters.palang.domain.book.application.BookSearchSort;
import com.nexters.palang.domain.book.application.BookService;
import com.nexters.palang.domain.book.application.ExternalBookResult;
import com.nexters.palang.domain.book.application.OpinionCountScope;
import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.book.domain.ReadingStatus;
import com.nexters.palang.domain.book.domain.UserBookStatus;
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
import org.springframework.mock.web.MockMultipartFile;
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
        given(bookService.searchInternalBooks(eq("제목"), any(BookSearchSort.class), any(Pageable.class))).willReturn(
                new PageImpl<>(List.of(new BookSearchProjection(
                        1L, "제목", "작가", "출판사", 300, "isbn", "cover", BookSource.MANUAL, 12, 34)),
                        DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/books/internal-search").param("keyword", "제목"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.books[0].bookId").value(1))
                .andExpect(jsonPath("$.data.books[0].title").value("제목"))
                .andExpect(jsonPath("$.data.books[0].passageCount").value(12))
                .andExpect(jsonPath("$.data.books[0].opinionCount").value(34));
    }

    @Test
    @DisplayName("필수 항목을 채워 도서를 직접 등록하면 등록된 도서를 반환한다")
    void createBook() throws Exception {
        CreateBookRequest request = new CreateBookRequest("제목", "작가", "출판사", 300, "isbn");
        MockMultipartFile bookPart = new MockMultipartFile(
                "book", "book", "application/json", objectMapper.writeValueAsString(request).getBytes());
        given(bookService.createBook(any(), any())).willReturn(book(1L, "제목"));

        mockMvc.perform(multipart("/api/books").file(bookPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookId").value(1));
    }

    @Test
    @DisplayName("표지 이미지와 함께 도서를 직접 등록하면 등록된 도서를 반환한다")
    void createBookWithCoverImage() throws Exception {
        CreateBookRequest request = new CreateBookRequest("제목", "작가", "출판사", 300, "isbn");
        MockMultipartFile bookPart = new MockMultipartFile(
                "book", "book", "application/json", objectMapper.writeValueAsString(request).getBytes());
        MockMultipartFile coverImagePart = new MockMultipartFile(
                "coverImage", "cover.jpg", "image/jpeg", "image-bytes".getBytes());
        given(bookService.createBook(any(), any())).willReturn(book(1L, "제목"));

        mockMvc.perform(multipart("/api/books").file(bookPart).file(coverImagePart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookId").value(1));
    }

    @Test
    @DisplayName("필수 항목이 비어 있는 상태로 도서 등록을 요청하면 400 에러가 발생한다")
    void createBookFailsWhenRequiredFieldIsBlank() throws Exception {
        CreateBookRequest request = new CreateBookRequest("", "작가", "출판사", 300, null);
        MockMultipartFile bookPart = new MockMultipartFile(
                "book", "book", "application/json", objectMapper.writeValueAsString(request).getBytes());

        mockMvc.perform(multipart("/api/books").file(bookPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }

    @Test
    @DisplayName("홈 캐러셀 도서 목록을 요청하면 대목/흔적 수와 함께 반환한다")
    void getHomeCarouselBooks() throws Exception {
        given(bookService.getHomeCarouselBooks(isNull(), anyInt())).willReturn(
                new BookCarouselPage(List.of(new BookActivityProjection(1L, "제목", "작가", "출판사", "cover", 3, 7)), 0, 20, 1));

        mockMvc.perform(get("/api/home/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.books[0].passageCount").value(3))
                .andExpect(jsonPath("$.data.books[0].opinionCount").value(7));
    }

    @Test
    @DisplayName("홈 캐러셀 도서 목록 조회 시 offset을 지정하면 그대로 전달하고, 응답에 이전/다음 여부를 함께 내려준다")
    void getHomeCarouselBooksWithOffset() throws Exception {
        given(bookService.getHomeCarouselBooks(eq(40L), anyInt())).willReturn(
                new BookCarouselPage(List.of(new BookActivityProjection(1L, "제목", "작가", "출판사", "cover", 3, 7)), 40, 20, 100));

        mockMvc.perform(get("/api/home/books").param("offset", "40").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageInfo.offset").value(40))
                .andExpect(jsonPath("$.data.pageInfo.hasPrevious").value(true))
                .andExpect(jsonPath("$.data.pageInfo.hasNext").value(true));
    }

    @Test
    @DisplayName("내 서재 도서 목록을 요청하면 현재 사용자 기준, opinionCountScope 기본값 ALL로 조회한다")
    void getMyLibraryBooks() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(bookService.getMyLibraryBooks(eq(1L), any(Pageable.class), eq(OpinionCountScope.ALL))).willReturn(
                new PageImpl<>(List.of(new BookActivityProjection(1L, "제목", "작가", "출판사", "cover", 3, 7)),
                        DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/books/my-library"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.books[0].bookId").value(1))
                .andExpect(jsonPath("$.data.books[0].passageCount").value(3))
                .andExpect(jsonPath("$.data.books[0].opinionCount").value(7));
    }

    @Test
    @DisplayName("내 서재 도서 목록 조회 시 opinionCountScope=MINE을 지정하면 그대로 서비스에 전달한다")
    void getMyLibraryBooksWithMineScope() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(bookService.getMyLibraryBooks(eq(1L), any(Pageable.class), eq(OpinionCountScope.MINE))).willReturn(
                new PageImpl<>(List.of(new BookActivityProjection(1L, "제목", "작가", "출판사", "cover", 3, 2)),
                        DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/books/my-library").param("opinionCountScope", "MINE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.books[0].opinionCount").value(2));
    }

    @Test
    @DisplayName("인증 없이 내 서재 도서 목록을 요청하면 401 에러가 발생한다")
    void getMyLibraryBooksFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(get("/api/books/my-library"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
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
                new PageImpl<>(List.of(new BookActivityProjection(1L, "인기 도서", "작가", "출판사", "cover", 3, 10)),
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

    @Test
    @DisplayName("로그인한 사용자가 도서 단건을 조회하면 myStatus/myCurrentPage를 함께 반환한다")
    void getBookDetailIncludesMyStatusWhenLoggedIn() throws Exception {
        given(currentUserProvider.findCurrentUserId()).willReturn(java.util.Optional.of(1L));
        BookDetailProjection projection = new BookDetailProjection(1L, "제목", "작가", "출판사", 300, "cover", 3, 7);
        UserBookStatus status = UserBookStatus.builder()
                .user(null).book(book(1L, "제목")).status(ReadingStatus.READING).currentPage(87).build();
        given(bookService.getBookDetail(1L, 1L)).willReturn(new BookDetail(projection, status));

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookId").value(1))
                .andExpect(jsonPath("$.data.publisher").value("출판사"))
                .andExpect(jsonPath("$.data.myStatus").value("READING"))
                .andExpect(jsonPath("$.data.myCurrentPage").value(87));
    }

    @Test
    @DisplayName("인증 없이 도서 단건을 조회하면 myStatus 없이 도서 메타만 반환한다")
    void getBookDetailWithoutAuthOmitsMyStatus() throws Exception {
        given(currentUserProvider.findCurrentUserId()).willReturn(java.util.Optional.empty());
        BookDetailProjection projection = new BookDetailProjection(1L, "제목", "작가", "출판사", 300, "cover", 3, 7);
        given(bookService.getBookDetail(1L, null)).willReturn(new BookDetail(projection, null));

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookId").value(1))
                .andExpect(jsonPath("$.data.myStatus").doesNotExist())
                .andExpect(jsonPath("$.data.myCurrentPage").doesNotExist())
                // jsonPath(...).doesNotExist()는 값이 null이어도(키는 존재) 통과하므로, 필드 자체가
                // 응답 JSON에서 빠졌는지는 원문 문자열로 별도 검증한다.
                .andExpect(content().string(not(containsString("myStatus"))))
                .andExpect(content().string(not(containsString("myCurrentPage"))));
    }

    @Test
    @DisplayName("존재하지 않는 도서를 조회하면 404 에러가 발생한다")
    void getBookDetailReturns404WhenBookNotFound() throws Exception {
        given(currentUserProvider.findCurrentUserId()).willReturn(java.util.Optional.empty());
        given(bookService.getBookDetail(999L, null)).willThrow(new BookException(BookErrorCode.BOOK_NOT_FOUND));

        mockMvc.perform(get("/api/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("BOOK_404_1"));
    }
}
