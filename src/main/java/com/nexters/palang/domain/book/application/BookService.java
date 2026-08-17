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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    // 아래 DB 조회들은 이 메서드 안에서 트랜잭션 없이 실행되지만, 전부 단순 조회(SELECT)라 문제 없다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Page<BookSearchProjection> searchBooks(String keyword, Pageable pageable) {
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        if (trimmedKeyword.length() < EXTERNAL_SEARCH_MIN_KEYWORD_LENGTH) {
            return Page.empty(pageable);
        }

        int size = pageable.getPageSize();
        long globalStart = pageable.getOffset();

        // DB 도서 전체(흔적 많은 순 정렬)와 알라딘 결과 전체를 이어붙인 하나의 목록이라고 보고, 요청받은
        // 페이지 구간([globalStart, globalStart+size))이 그 목록의 어느 지점에 해당하는지 계산해서 DB/알라딘
        // 양쪽에 정확한 offset/limit을 나눠준다. DB 매칭이 몇 건이든(페이지 크기를 넘어가도) 페이지를
        // 넘기다 보면 전부 노출되고, 같은 도서가 페이지마다 중복 노출되는 일도 없다.
        long dbTotal = bookQueryRepository.countByTitle(trimmedKeyword);
        int dbCountThisPage = (int) Math.min(Math.max(dbTotal - globalStart, 0), size);
        int aladinLimitForContent = size - dbCountThisPage;
        long aladinOffset = Math.max(globalStart - dbTotal, 0);

        List<BookSearchProjection> dbBooks = dbCountThisPage > 0
                ? bookQueryRepository.searchByTitle(trimmedKeyword, BookSearchSort.OPINION, globalStart, dbCountThisPage)
                : List.of();

        // 알라딘 ItemSearch는 임의의 레코드 offset을 직접 요청할 방법이 없어(start는 페이지 번호,
        // MaxResults 단위로만 페이지가 나뉨), AladinBookApiClient#searchAll이 키워드당 결과 전체
        // (최대 200건, 알라딘 한도)를 미리 모아 캐싱해서 돌려준다. 여기서 그 리스트를 대상으로
        // offset/limit 슬라이싱을 직접 수행한다.
        AladinSearchResult aladinAll = aladinBookApiClient.searchAll(trimmedKeyword);

        // 같은 책이 알라딘/DB 양쪽에 모두 있으면(ISBN 동일) 이미 등록된 DB 쪽만 남기고 알라딘 쪽은 제외한다.
        // ISBN이 없는 알라딘 결과는 비교 기준이 없으므로 그대로 둔다. dedup을 먼저 하고 나서 offset/limit을
        // 적용하므로, 중복 때문에 페이지가 size보다 적게 채워지는 일도 없다.
        Set<String> registeredIsbns = Set.copyOf(bookQueryRepository.findIsbnsByTitle(trimmedKeyword));
        List<BookSearchProjection> aladinBooks = aladinAll.items().stream()
                .filter(item -> item.isbn() == null || item.isbn().isBlank() || !registeredIsbns.contains(item.isbn()))
                .skip(aladinOffset)
                .limit(aladinLimitForContent)
                .map(BookSearchProjection::from)
                .toList();

        List<BookSearchProjection> content = new ArrayList<>(dbBooks);
        content.addAll(aladinBooks);

        long total = dbTotal + aladinAll.totalResults();
        return new PageImpl<>(content, pageable, total);
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
