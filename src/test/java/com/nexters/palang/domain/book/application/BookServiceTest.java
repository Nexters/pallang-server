package com.nexters.palang.domain.book.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.book.infrastructure.AladinBookApiClient;
import com.nexters.palang.domain.book.infrastructure.BookQueryRepository;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.book.presentation.dto.CreateBookRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookQueryRepository bookQueryRepository;

    @Mock
    private AladinBookApiClient aladinBookApiClient;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, bookQueryRepository, aladinBookApiClient);
    }

    private Book book(Long id, String title) {
        Book book = Book.builder()
                .title(title)
                .author("작가")
                .publisher("출판사")
                .pageCount(300)
                .build();
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }

    @Test
    @DisplayName("키워드로 외부 검색을 하면 알라딘 API 클라이언트의 결과를 그대로 반환한다")
    void searchExternalBooks() {
        List<ExternalBookResult> expected = List.of(
                new ExternalBookResult("제목", "작가", "출판사", "isbn", "cover"));
        given(aladinBookApiClient.search("제목")).willReturn(expected);

        List<ExternalBookResult> results = bookService.searchExternalBooks("제목");

        assertThat(results).isEqualTo(expected);
    }

    @Test
    @DisplayName("키워드로 내부 도서를 검색하면 제목에 포함된 도서 목록을 반환한다")
    void searchInternalBooks() {
        Book book = book(1L, "프랑켄슈타인");
        given(bookRepository.findByTitleContainingIgnoreCase("프랑")).willReturn(List.of(book));

        List<Book> results = bookService.searchInternalBooks("프랑");

        assertThat(results).containsExactly(book);
    }

    @Test
    @DisplayName("도서를 직접 등록하면 출처가 MANUAL인 도서로 저장된다")
    void createBook() {
        CreateBookRequest request = new CreateBookRequest("제목", "작가", "출판사", 300, "isbn", "cover");
        given(bookRepository.save(any(Book.class))).willAnswer(invocation -> invocation.getArgument(0));

        Book created = bookService.createBook(request);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(BookSource.MANUAL);
        assertThat(created.getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("내가 최근에 남긴 도서 목록은 최근 활동 순서를 그대로 유지한다")
    void getRecentBooksKeepsActivityOrder() {
        Book book1 = book(1L, "책1");
        Book book2 = book(2L, "책2");
        Book book3 = book(3L, "책3");
        given(bookQueryRepository.findRecentlyActiveBookIds(anyLong(), anyInt()))
                .willReturn(List.of(3L, 1L, 2L));
        given(bookRepository.findAllById(List.of(3L, 1L, 2L)))
                .willReturn(List.of(book1, book2, book3));

        List<Book> results = bookService.getRecentBooks(10L);

        assertThat(results).containsExactly(book3, book1, book2);
    }

    @Test
    @DisplayName("인기 도서 목록을 조회하면 QueryRepository 결과를 그대로 반환한다")
    void getPopularBooks() {
        List<BookActivityProjection> expected = List.of(
                new BookActivityProjection(1L, "책1", "작가", "cover", 5, 10));
        given(bookQueryRepository.findPopularBooks(anyInt())).willReturn(expected);

        List<BookActivityProjection> results = bookService.getPopularBooks();

        assertThat(results).isEqualTo(expected);
    }
}
