package com.nexters.palang.domain.book.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        Pageable pageable = PageRequest.of(0, 20);
        Page<ExternalBookResult> expected = new PageImpl<>(
                List.of(new ExternalBookResult("제목", "작가", "출판사", 300, "isbn", "cover")), pageable, 1);
        given(aladinBookApiClient.search("제목", pageable)).willReturn(expected);

        Page<ExternalBookResult> results = bookService.searchExternalBooks("제목", pageable);

        assertThat(results).isEqualTo(expected);
    }

    @Test
    @DisplayName("키워드로 내부 도서를 검색하면 제목에 포함된 도서 목록을 반환한다")
    void searchInternalBooks() {
        Pageable pageable = PageRequest.of(0, 20);
        Book book = book(1L, "프랑켄슈타인");
        given(bookRepository.findByTitleContainingIgnoreCase("프랑", pageable))
                .willReturn(new PageImpl<>(List.of(book), pageable, 1));

        Page<Book> results = bookService.searchInternalBooks("프랑", pageable);

        assertThat(results.getContent()).containsExactly(book);
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
        Pageable pageable = PageRequest.of(0, 20);
        Book book1 = book(1L, "책1");
        Book book2 = book(2L, "책2");
        Book book3 = book(3L, "책3");
        given(bookQueryRepository.findRecentlyActiveBookIds(10L, pageable))
                .willReturn(new PageImpl<>(List.of(3L, 1L, 2L), pageable, 3));
        given(bookRepository.findAllById(List.of(3L, 1L, 2L)))
                .willReturn(List.of(book1, book2, book3));

        Page<Book> results = bookService.getRecentBooks(10L, pageable);

        assertThat(results.getContent()).containsExactly(book3, book1, book2);
        assertThat(results.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("인기 도서 목록을 조회하면 QueryRepository 결과를 그대로 반환한다")
    void getPopularBooks() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<BookActivityProjection> expected = new PageImpl<>(
                List.of(new BookActivityProjection(1L, "책1", "작가", "cover", 5, 10)), pageable, 1);
        given(bookQueryRepository.findPopularBooks(pageable)).willReturn(expected);

        Page<BookActivityProjection> results = bookService.getPopularBooks(pageable);

        assertThat(results).isEqualTo(expected);
    }
}
