package com.nexters.palang.domain.book.application;

import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.book.infrastructure.AladinBookApiClient;
import com.nexters.palang.domain.book.infrastructure.AladinSearchResult;
import com.nexters.palang.domain.book.infrastructure.BookQueryRepository;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.book.presentation.dto.CreateBookRequest;
import com.nexters.palang.global.storage.FileStorageService;
import com.nexters.palang.global.storage.ImageMimeType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private static final String COVER_IMAGE_SUB_DIRECTORY = "book-covers";
    // 1글자 검색은 자동완성 노이즈만 크고 알라딘 쿼터만 소모하므로, 호출 자체를 하지 않는다.
    private static final int EXTERNAL_SEARCH_MIN_KEYWORD_LENGTH = 2;

    private final BookRepository bookRepository;
    private final BookQueryRepository bookQueryRepository;
    private final AladinBookApiClient aladinBookApiClient;
    private final FileStorageService fileStorageService;

    // 알라딘 API 호출(최대 수 초)이 포함돼 있어, DB 커넥션을 불필요하게 오래 붙잡지 않도록 트랜잭션에서 제외한다.
    // 아래 DB 조회(searchByTitle)는 이 메서드 안에서 트랜잭션 없이 실행되지만, 단순 조회(SELECT)라 문제 없다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Page<BookSearchProjection> searchBooks(String keyword, Pageable pageable) {
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        if (trimmedKeyword.length() < EXTERNAL_SEARCH_MIN_KEYWORD_LENGTH) {
            return Page.empty(pageable);
        }

        // 알라딘은 요청받은 page/size를 그대로 써서 조회한다(알라딘 자체 페이지 커서를 그대로 신뢰).
        AladinSearchResult aladinResult = aladinBookApiClient.search(trimmedKeyword, pageable);

        // 같은 책이 알라딘/DB 양쪽에 모두 있으면(ISBN 동일) 이미 등록된 DB 쪽만 남기고 알라딘 쪽은 페이지와
        // 무관하게 항상 제외한다(findIsbnsByTitle은 페이지로 잘리지 않는 전체 매칭 ISBN 목록이라 2페이지
        // 이후에도 정확하게 걸러낼 수 있다). ISBN이 없는 알라딘 결과는 비교 기준이 없으므로 그대로 둔다.
        Set<String> registeredIsbns = Set.copyOf(bookQueryRepository.findIsbnsByTitle(trimmedKeyword));
        List<BookSearchProjection> aladinBooks = aladinResult.items().stream()
                .filter(item -> item.isbn() == null || item.isbn().isBlank() || !registeredIsbns.contains(item.isbn()))
                .map(BookSearchProjection::from)
                .toList();

        if (pageable.getPageNumber() > 0) {
            // 2페이지부터는 1페이지에서 이미 DB 매칭 도서를 전부 보여줬으므로 알라딘 결과만 채운다.
            // (그렇지 않으면 같은 DB 도서가 페이지마다 반복해서 노출된다.) total은 1페이지와 같은 방식
            // (DB total + 알라딘 total)으로 계산해, 페이지를 넘겨도 total이 흔들리지 않게 한다.
            long dbTotal = bookQueryRepository.countByTitle(trimmedKeyword);
            return new PageImpl<>(aladinBooks, pageable, dbTotal + aladinResult.totalResults());
        }

        // 1페이지에서만 DB에 등록된 도서(수동 등록 포함)를 함께 보여준다.
        Page<BookSearchProjection> dbResult = bookQueryRepository.searchByTitle(
                trimmedKeyword, BookSearchSort.OPINION, PageRequest.of(0, pageable.getPageSize()));

        List<BookSearchProjection> merged = new ArrayList<>(dbResult.getContent());
        merged.addAll(aladinBooks);
        // 흔적(Opinion) 많은 순으로 정렬한다. 알라딘 결과는 opinionCount가 항상 0이라 자연히 DB 도서보다
        // 뒤로 밀리고, 0으로 동점인 도서끼리는 stable sort라 DB가 먼저 오는 원래 순서가 유지된다.
        merged.sort(Comparator.comparingLong(BookSearchProjection::opinionCount).reversed());
        List<BookSearchProjection> content = merged.stream().limit(pageable.getPageSize()).toList();

        // 알라딘/DB 결과가 겹칠 수 있어 정확한 합산은 아니지만, 중복 제거 건수를 그때그때 반영하지 않고
        // 페이지마다 동일한 방식(단순 합)으로 계산해 total이 페이지 이동에 따라 들쭉날쭉하지 않게 한다.
        long approximateTotal = dbResult.getTotalElements() + aladinResult.totalResults();
        return new PageImpl<>(content, pageable, approximateTotal);
    }

    public Page<BookSearchProjection> searchInternalBooks(String keyword, BookSearchSort sort, Pageable pageable) {
        return bookQueryRepository.searchByTitle(keyword, sort, pageable);
    }

    @Transactional
    public Book createBook(CreateBookRequest request, MultipartFile coverImage) {
        String coverImageUrl = coverImage != null && !coverImage.isEmpty()
                ? storeCoverImage(coverImage)
                : null;
        Book book = Book.builder()
                .title(request.title())
                .author(request.author())
                .publisher(request.publisher())
                .pageCount(request.pageCount())
                .isbn(request.isbn())
                .coverImageUrl(coverImageUrl)
                .source(BookSource.MANUAL)
                .build();
        return bookRepository.save(book);
    }

    private String storeCoverImage(MultipartFile coverImage) {
        if (ImageMimeType.from(coverImage.getContentType()).isEmpty()) {
            throw new BookException(BookErrorCode.INVALID_IMAGE_FILE);
        }
        return fileStorageService.store(coverImage, COVER_IMAGE_SUB_DIRECTORY);
    }

    // offset을 지정하지 않으면 전체 목록 중 가운데 책들을 반환한다. 좌우 스크롤 시에는 이전/다음 offset을 그대로 넘기면 된다.
    public BookCarouselPage getHomeCarouselBooks(Long offset, int size) {
        long total = bookQueryRepository.countCarouselBooks();
        long resolvedOffset = offset != null ? Math.max(0, offset) : centerOffset(total, size);
        List<BookActivityProjection> books = bookQueryRepository.findCarouselBooks(resolvedOffset, size);
        return new BookCarouselPage(books, resolvedOffset, size, total);
    }

    private long centerOffset(long total, int size) {
        return Math.max(0, (total - size) / 2);
    }

    // 내 서재는 홈 캐러셀과 달리 가운데 기준 없이 최근 흔적 순으로 나열하며, 표준 page/size 페이지네이션을 사용한다.
    public Page<BookActivityProjection> getMyLibraryBooks(Long userId, Pageable pageable, OpinionCountScope opinionCountScope) {
        return bookQueryRepository.findMyLibraryBooks(userId, pageable, opinionCountScope);
    }

    public Page<Book> getRecentBooks(Long userId, Pageable pageable) {
        Page<Long> bookIds = bookQueryRepository.findRecentlyActiveBookIds(userId, pageable);
        Map<Long, Book> booksById = bookRepository.findAllById(bookIds.getContent()).stream()
                .collect(Collectors.toMap(Book::getId, book -> book));
        List<Book> books = bookIds.getContent().stream().map(booksById::get).filter(Objects::nonNull).toList();
        return new PageImpl<>(books, pageable, bookIds.getTotalElements());
    }

    public Page<BookActivityProjection> getPopularBooks(Pageable pageable) {
        return bookQueryRepository.findPopularBooks(pageable);
    }
}
