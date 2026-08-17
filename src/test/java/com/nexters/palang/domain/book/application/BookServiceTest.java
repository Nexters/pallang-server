package com.nexters.palang.domain.book.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.book.infrastructure.AladinBookApiClient;
import com.nexters.palang.domain.book.infrastructure.AladinSearchResult;
import com.nexters.palang.domain.book.infrastructure.BookQueryRepository;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.presentation.dto.CreateBookRequest;
import com.nexters.palang.global.storage.FileStorageService;
import java.util.ArrayList;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookQueryRepository bookQueryRepository;

    @Mock
    private AladinBookApiClient aladinBookApiClient;

    @Mock
    private FileStorageService fileStorageService;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, bookQueryRepository, aladinBookApiClient, fileStorageService);
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

    // fromId..toId(포함) 범위의 더미 DB 검색 결과를 생성한다. id별로 isbn도 다르게 채워서 dedup에
    // 우연히 걸리지 않게 한다.
    private List<BookSearchProjection> dbBooks(int fromId, int toId) {
        List<BookSearchProjection> books = new ArrayList<>();
        for (int id = fromId; id <= toId; id++) {
            books.add(new BookSearchProjection(
                    (long) id, "책" + id, "작가", "출판사", 300, "isbn-db-" + id, "cover-" + id,
                    BookSource.MANUAL, 0, id));
        }
        return books;
    }

    @Test
    @DisplayName("검색어 앞뒤 공백은 제거하고 알라딘을 호출한다")
    void searchBooksTrimsKeyword() {
        Pageable pageable = PageRequest.of(0, 20);
        given(bookQueryRepository.countByTitle("제목")).willReturn(0L);
        given(aladinBookApiClient.search("제목", 0, 20)).willReturn(AladinSearchResult.empty());

        bookService.searchBooks("  제목  ", pageable);

        verify(aladinBookApiClient).search("제목", 0, 20);
    }

    @Test
    @DisplayName("검색어가 2글자 미만이면 알라딘/DB 모두 조회하지 않고 빈 결과를 반환한다")
    void searchBooksReturnsEmptyWhenKeywordTooShort() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<BookSearchProjection> results = bookService.searchBooks("책", pageable);

        assertThat(results.getContent()).isEmpty();
        verifyNoInteractions(aladinBookApiClient, bookQueryRepository);
    }

    @Test
    @DisplayName("DB 매칭 도서를 앞쪽 slot에 채우고, 나머지 slot을 알라딘 결과로 채운다")
    void searchBooksFillsDbSlotsFirstThenAladin() {
        Pageable pageable = PageRequest.of(0, 20);
        BookSearchProjection dbBook = new BookSearchProjection(
                1L, "프랑켄슈타인", "메리 셸리", "민음사", 300, "isbn-db", "cover-db", BookSource.MANUAL, 3, 5);
        ExternalBookResult aladinBook = new ExternalBookResult(
                "프랑켄슈타인", "메리 셸리", "문학동네", "isbn-aladin", "cover-aladin");
        given(bookQueryRepository.countByTitle("프랑켄슈타인")).willReturn(1L);
        given(bookQueryRepository.searchByTitle("프랑켄슈타인", BookSearchSort.OPINION, 0L, 1))
                .willReturn(List.of(dbBook));
        // DB가 1건이라 이 페이지에서 알라딘 slot은 19개(size 20 - db 1) 필요하다.
        given(aladinBookApiClient.search("프랑켄슈타인", 0, 19))
                .willReturn(new AladinSearchResult(List.of(aladinBook), 1));

        Page<BookSearchProjection> results = bookService.searchBooks("프랑켄슈타인", pageable);

        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent().get(0).bookId()).isEqualTo(1L);
        assertThat(results.getContent().get(1).bookId()).isNull();
        assertThat(results.getContent().get(1).source()).isNull();
        assertThat(results.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("ISBN이 같은 도서가 DB와 알라딘 양쪽에 있으면 DB에 등록된 도서만 남기고 알라딘 쪽은 제외한다")
    void searchBooksDropsAladinDuplicateWhenIsbnMatchesRegisteredBook() {
        Pageable pageable = PageRequest.of(0, 20);
        BookSearchProjection dbBook = new BookSearchProjection(
                1L, "프랑켄슈타인", "메리 셸리", "민음사", 300, "isbn-1234", "cover-db", BookSource.MANUAL, 3, 5);
        ExternalBookResult duplicateAladinBook = new ExternalBookResult(
                "프랑켄슈타인", "메리 셸리", "민음사", "isbn-1234", "cover-aladin");
        ExternalBookResult otherAladinBook = new ExternalBookResult(
                "프랑켄슈타인", "메리 셸리", "문학동네", "isbn-5678", "cover-aladin-2");
        given(bookQueryRepository.countByTitle("프랑켄슈타인")).willReturn(1L);
        given(bookQueryRepository.searchByTitle("프랑켄슈타인", BookSearchSort.OPINION, 0L, 1))
                .willReturn(List.of(dbBook));
        given(bookQueryRepository.findIsbnsByTitle("프랑켄슈타인")).willReturn(List.of("isbn-1234"));
        given(aladinBookApiClient.search("프랑켄슈타인", 0, 19)).willReturn(
                new AladinSearchResult(List.of(duplicateAladinBook, otherAladinBook), 2));

        Page<BookSearchProjection> results = bookService.searchBooks("프랑켄슈타인", pageable);

        assertThat(results.getContent()).extracting(BookSearchProjection::isbn)
                .containsExactly("isbn-1234", "isbn-5678");
        assertThat(results.getContent()).extracting(BookSearchProjection::bookId)
                .containsExactly(1L, null);
    }

    @Test
    @DisplayName("DB 매칭이 페이지 크기보다 많으면 다음 페이지에서 이어지는 DB 도서를 보여주고, 같은 도서가 중복 노출되지 않는다")
    void searchBooksPaginatesAcrossDbOverflowIntoNextPage() {
        // DB 매칭 25건, size 20 → 1페이지는 DB로 꽉 차서 알라딘 slot이 0개(total 파악용으로 알라딘은
        // 최소 1건만 요청), 2페이지는 DB 나머지 5건(21~25번) + 알라딘 15건으로 채워진다.
        List<BookSearchProjection> firstPageDbBooks = dbBooks(1, 20);
        List<BookSearchProjection> secondPageDbBooks = dbBooks(21, 25);
        ExternalBookResult aladinBook = new ExternalBookResult("책모음", "작가", "출판사", "isbn-a", "cover-a");
        given(bookQueryRepository.countByTitle("책모음")).willReturn(25L);
        given(bookQueryRepository.searchByTitle("책모음", BookSearchSort.OPINION, 0L, 20))
                .willReturn(firstPageDbBooks);
        given(bookQueryRepository.searchByTitle("책모음", BookSearchSort.OPINION, 20L, 5))
                .willReturn(secondPageDbBooks);
        given(aladinBookApiClient.search("책모음", 0, 1)).willReturn(new AladinSearchResult(List.of(), 7));
        given(aladinBookApiClient.search("책모음", 0, 15))
                .willReturn(new AladinSearchResult(List.of(aladinBook), 7));

        Page<BookSearchProjection> firstPage = bookService.searchBooks("책모음", PageRequest.of(0, 20));
        Page<BookSearchProjection> secondPage = bookService.searchBooks("책모음", PageRequest.of(1, 20));

        assertThat(firstPage.getContent()).isEqualTo(firstPageDbBooks);

        assertThat(secondPage.getContent()).hasSize(6); // DB 5건 + 알라딘 1건
        assertThat(secondPage.getContent().subList(0, 5)).isEqualTo(secondPageDbBooks);
        assertThat(secondPage.getContent().get(5).bookId()).isNull();

        // 1페이지에서 이미 보여준 DB 도서(1~20번)가 2페이지에 다시 나오지 않는다.
        List<Long> firstPageIds = firstPage.getContent().stream().map(BookSearchProjection::bookId).toList();
        assertThat(secondPage.getContent()).extracting(BookSearchProjection::bookId)
                .doesNotContainAnyElementsOf(firstPageIds);
    }

    @Test
    @DisplayName("DB 매칭이 1페이지 안에서 끝나면 2페이지부터는 알라딘 결과만 채우고, 알라딘 offset은 DB total만큼 당겨서 계산한다")
    void searchBooksReturnsAladinOnlyWhenDbFitsInFirstPage() {
        Pageable pageable = PageRequest.of(1, 20);
        ExternalBookResult duplicateAladinBook = new ExternalBookResult("제목", "작가", "출판사", "isbn-dup", "cover");
        ExternalBookResult newAladinBook = new ExternalBookResult("제목", "작가", "출판사2", "isbn-new", "cover2");
        given(bookQueryRepository.countByTitle("제목")).willReturn(5L);
        given(bookQueryRepository.findIsbnsByTitle("제목")).willReturn(List.of("isbn-dup"));
        // DB total이 5라 2페이지(offset 20)에서는 DB slot이 0개이고, 알라딘 offset은 20-5=15부터 시작한다.
        given(aladinBookApiClient.search("제목", 15, 20))
                .willReturn(new AladinSearchResult(List.of(duplicateAladinBook, newAladinBook), 21));

        Page<BookSearchProjection> results = bookService.searchBooks("제목", pageable);

        assertThat(results.getContent()).extracting(BookSearchProjection::isbn).containsExactly("isbn-new");
        verify(bookQueryRepository, never()).searchByTitle(anyString(), any(BookSearchSort.class), anyLong(), anyInt());
    }

    @Test
    @DisplayName("키워드로 내부 도서를 검색하면 QueryRepository 결과를 그대로 반환한다")
    void searchInternalBooks() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<BookSearchProjection> expected = new PageImpl<>(
                List.of(new BookSearchProjection(
                        1L, "프랑켄슈타인", "작가", "출판사", 300, "isbn", "cover", BookSource.MANUAL, 5, 10)),
                pageable, 1);
        given(bookQueryRepository.searchByTitle("프랑", BookSearchSort.RECENT, pageable)).willReturn(expected);

        Page<BookSearchProjection> results = bookService.searchInternalBooks("프랑", BookSearchSort.RECENT, pageable);

        assertThat(results).isEqualTo(expected);
    }

    @Test
    @DisplayName("표지 이미지 없이 도서를 직접 등록하면 출처가 MANUAL이고 coverImageUrl은 null이다")
    void createBook() {
        CreateBookRequest request = new CreateBookRequest("제목", "작가", "출판사", 300, "isbn");
        given(bookRepository.save(any(Book.class))).willAnswer(invocation -> invocation.getArgument(0));

        Book created = bookService.createBook(request, null);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(BookSource.MANUAL);
        assertThat(created.getTitle()).isEqualTo("제목");
        assertThat(created.getCoverImageUrl()).isNull();
    }

    @Test
    @DisplayName("표지 이미지를 함께 등록하면 저장소에 업로드하고 반환된 URL을 coverImageUrl로 저장한다")
    void createBookWithCoverImage() {
        CreateBookRequest request = new CreateBookRequest("제목", "작가", "출판사", 300, "isbn");
        MockMultipartFile coverImage = new MockMultipartFile("coverImage", "cover.jpg", "image/jpeg", "data".getBytes());
        given(fileStorageService.store(coverImage, "book-covers")).willReturn("https://storage.example.com/book-covers/uuid.jpg");
        given(bookRepository.save(any(Book.class))).willAnswer(invocation -> invocation.getArgument(0));

        Book created = bookService.createBook(request, coverImage);

        assertThat(created.getCoverImageUrl()).isEqualTo("https://storage.example.com/book-covers/uuid.jpg");
    }

    @Test
    @DisplayName("이미지가 아닌 파일을 표지로 올리면 예외가 발생한다")
    void createBookWithInvalidImageFileThrows() {
        CreateBookRequest request = new CreateBookRequest("제목", "작가", "출판사", 300, "isbn");
        MockMultipartFile invalidFile = new MockMultipartFile("coverImage", "cover.txt", "text/plain", "data".getBytes());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> bookService.createBook(request, invalidFile))
                .isInstanceOf(BookException.class);
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

    @Test
    @DisplayName("offset을 지정하지 않으면 전체 목록 중 가운데 위치를 offset으로 계산한다")
    void getHomeCarouselBooksResolvesCenterOffsetWhenOffsetIsNull() {
        given(bookQueryRepository.countCarouselBooks()).willReturn(100L);
        given(bookQueryRepository.findCarouselBooks(40L, 20))
                .willReturn(List.of(new BookActivityProjection(1L, "책1", "작가", "cover", 5, 10)));

        BookCarouselPage result = bookService.getHomeCarouselBooks(null, 20);

        assertThat(result.offset()).isEqualTo(40L);
        assertThat(result.totalElements()).isEqualTo(100L);
        assertThat(result.books()).hasSize(1);
    }

    @Test
    @DisplayName("offset을 직접 지정하면 그 값을 그대로 사용해 조회한다")
    void getHomeCarouselBooksUsesGivenOffset() {
        given(bookQueryRepository.countCarouselBooks()).willReturn(100L);
        given(bookQueryRepository.findCarouselBooks(60L, 20)).willReturn(List.of());

        BookCarouselPage result = bookService.getHomeCarouselBooks(60L, 20);

        assertThat(result.offset()).isEqualTo(60L);
    }

    @Test
    @DisplayName("전체 개수가 size보다 작으면 가운데 offset은 0이다")
    void getHomeCarouselBooksClampsCenterOffsetToZero() {
        given(bookQueryRepository.countCarouselBooks()).willReturn(5L);
        given(bookQueryRepository.findCarouselBooks(0L, 20)).willReturn(List.of());

        BookCarouselPage result = bookService.getHomeCarouselBooks(null, 20);

        assertThat(result.offset()).isEqualTo(0L);
    }

    @Test
    @DisplayName("내 서재 조회는 표준 page/size 페이지네이션과 opinionCountScope를 그대로 리포지토리에 위임한다")
    void getMyLibraryBooksDelegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<BookActivityProjection> expected = new PageImpl<>(
                List.of(new BookActivityProjection(1L, "책1", "작가", "cover", 5, 10)), pageable, 1);
        given(bookQueryRepository.findMyLibraryBooks(10L, pageable, OpinionCountScope.ALL)).willReturn(expected);

        Page<BookActivityProjection> results = bookService.getMyLibraryBooks(10L, pageable, OpinionCountScope.ALL);

        assertThat(results).isEqualTo(expected);
    }

    @Test
    @DisplayName("내 서재 조회는 opinionCountScope가 MINE이어도 그대로 리포지토리에 위임한다")
    void getMyLibraryBooksDelegatesMineScopeToRepository() {
        Pageable pageable = PageRequest.of(1, 20);
        Page<BookActivityProjection> expected = new PageImpl<>(List.of(), pageable, 0);
        given(bookQueryRepository.findMyLibraryBooks(10L, pageable, OpinionCountScope.MINE)).willReturn(expected);

        Page<BookActivityProjection> results = bookService.getMyLibraryBooks(10L, pageable, OpinionCountScope.MINE);

        assertThat(results).isEqualTo(expected);
    }
}
