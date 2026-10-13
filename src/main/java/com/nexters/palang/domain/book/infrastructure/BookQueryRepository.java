package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.application.BookActivityProjection;
import com.nexters.palang.domain.book.application.BookDetailProjection;
import com.nexters.palang.domain.book.application.BookSearchProjection;
import com.nexters.palang.domain.book.application.BookSearchSort;
import com.nexters.palang.domain.book.application.MyLibraryBookProjection;
import com.nexters.palang.domain.book.application.OpinionCountScope;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.QBook;
import com.nexters.palang.domain.book.domain.QUserBookStatus;
import com.nexters.palang.domain.group.domain.QGroup;
import com.nexters.palang.domain.group.domain.QGroupMember;
import com.nexters.palang.domain.opinion.domain.QOpinion;
import com.nexters.palang.domain.passage.domain.QPassage;
import com.nexters.palang.domain.user.domain.GuestSampleAccount;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BookQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 띄어쓰기 차이를 무시하고 검색할 수 있도록, 저장 시점에 미리 정규화(공백 제거 + 소문자)해둔
    // title_normalized 컬럼(인덱스 적용, Book#normalizeTitle 참고)을 기준으로 비교한다. 매 검색마다
    // title에 함수를 걸어 계산하지 않아도 되고, 자동완성처럼 반복 호출되는 경로에서 유리하다.
    // 흔적(Opinion)/대목(Passage) 유무와 무관하게 DB에 저장된 도서 전부를 대상으로 하므로 leftJoin을 사용한다.
    // 소프트 삭제된 대목/그 흔적은 leftJoin의 ON 절에서 제외한다 (WHERE에 두면 대목이 전부 삭제된
    // 도서까지 결과에서 사라진다 — ON에 두어야 매칭 실패로 취급되어 passageCount 0인 채로 남는다).
    public Page<BookSearchProjection> searchByTitle(String keyword, BookSearchSort sort, Pageable pageable) {
        List<BookSearchProjection> content = searchByTitle(keyword, sort, pageable.getOffset(), pageable.getPageSize());
        // 흔적/대목 유무로 필터링하지 않으므로 count 쪽은 join 없이 제목 조건만으로 센다.
        return new PageImpl<>(content, pageable, countByTitle(keyword));
    }

    // GET /api/books/search가 DB 검색 결과를 알라딘 결과와 하나의 연속된 목록처럼 취급해 페이지네이션할 때
    // 쓰는 오버로드다. Pageable은 offset이 page*size로 고정돼 임의의 offset을 표현할 수 없어
    // (findCarouselBooks와 동일한 이유) offset/limit을 직접 받는다.
    public List<BookSearchProjection> searchByTitle(String keyword, BookSearchSort sort, long offset, int limit) {
        QBook book = QBook.book;
        QPassage passage = QPassage.passage;
        QOpinion opinion = QOpinion.opinion;

        String normalizedKeyword = Book.normalize(keyword);

        return queryFactory
                .select(Projections.constructor(BookSearchProjection.class,
                        book.id, book.title, book.author, book.publisher, book.pageCount,
                        book.isbn, book.coverImageUrl, book.source,
                        passage.countDistinct(), opinion.countDistinct()))
                .from(book)
                .leftJoin(passage).on(passage.book.eq(book).and(passage.deletedAt.isNull()))
                .leftJoin(opinion).on(opinion.passage.eq(passage).and(opinion.deletedAt.isNull()))
                .where(book.titleNormalized.contains(normalizedKeyword))
                .groupBy(book.id, book.title, book.author, book.publisher, book.pageCount,
                        book.isbn, book.coverImageUrl, book.source)
                .orderBy(searchOrderSpecifiers(sort, book, opinion, normalizedKeyword))
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    // 제목으로 매칭되는 DB 도서의 전체 개수. join 없이 단순 COUNT라 자주 호출해도 부담이 적다.
    // (GET /api/books/search가 알라딘 결과와 합쳐 total을 계산할 때 페이지마다 이 값을 다시 구한다.)
    public long countByTitle(String keyword) {
        QBook book = QBook.book;
        String normalizedKeyword = Book.normalize(keyword);

        Long count = queryFactory
                .select(book.countDistinct())
                .from(book)
                .where(book.titleNormalized.contains(normalizedKeyword))
                .fetchOne();
        return count != null ? count : 0L;
    }

    // GET /api/books/search에서 알라딘 결과 중 이미 DB에 등록된 도서와 ISBN이 같은 항목을 걸러내기 위한
    // 용도다. join/groupBy 없이 isbn 컬럼만 조회하는 가벼운 쿼리라, 페이지와 무관하게 매 요청마다 호출해도
    // 부담이 적다. searchByTitle과 달리 페이지 크기로 잘리지 않고 매칭되는 도서 전부의 ISBN을 반환한다.
    public List<String> findIsbnsByTitle(String keyword) {
        QBook book = QBook.book;
        String normalizedKeyword = Book.normalize(keyword);

        return queryFactory
                .select(book.isbn)
                .from(book)
                .where(book.titleNormalized.contains(normalizedKeyword), book.isbn.isNotNull(), book.isbn.ne(""))
                .fetch();
    }

    // 자동완성으로 쓰일 때 관련도가 높은 결과(제목이 검색어로 시작하는 도서)가 먼저 보이도록,
    // 선택된 정렬 기준보다 접두어 일치 여부를 우선한다.
    private OrderSpecifier<?>[] searchOrderSpecifiers(
            BookSearchSort sort, QBook book, QOpinion opinion, String normalizedKeyword) {
        OrderSpecifier<Integer> prefixMatchFirst = new CaseBuilder()
                .when(book.titleNormalized.startsWith(normalizedKeyword)).then(0)
                .otherwise(1)
                .asc();

        OrderSpecifier<?>[] tieBreakers = switch (sort) {
            case NAME -> new OrderSpecifier[]{book.title.asc(), book.id.asc()};
            case RECENT -> new OrderSpecifier[]{opinion.createdAt.max().desc(), book.id.asc()};
            case OPINION -> new OrderSpecifier[]{opinion.countDistinct().desc(), book.id.asc()};
        };

        OrderSpecifier<?>[] result = new OrderSpecifier<?>[tieBreakers.length + 1];
        result[0] = prefixMatchFirst;
        System.arraycopy(tieBreakers, 0, result, 1, tieBreakers.length);
        return result;
    }

    // 홈 캐러셀은 전체 목록 중 임의의 가운데 offset에서 좌우로 조회해야 해서, page*size로 offset이
    // 고정되는 Pageable 대신 offset/limit을 직접 받는다.
    public List<BookActivityProjection> findCarouselBooks(long offset, int limit) {
        QBook book = QBook.book;
        QPassage passage = QPassage.passage;
        QOpinion opinion = QOpinion.opinion;

        return queryFactory
                .select(Projections.constructor(BookActivityProjection.class,
                        book.id, book.title, book.author, book.publisher, book.coverImageUrl,
                        passage.countDistinct(), opinion.countDistinct()))
                .from(book)
                .innerJoin(passage).on(passage.book.eq(book).and(passage.deletedAt.isNull()))
                .leftJoin(opinion).on(opinion.passage.eq(passage).and(opinion.deletedAt.isNull()))
                .groupBy(book.id, book.title, book.author, book.publisher, book.coverImageUrl)
                .orderBy(passage.createdAt.max().desc(), book.id.asc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public long countCarouselBooks() {
        return countBooksWithPassages();
    }

    // 내 서재는 로그인한 사용자가 흔적(Opinion)을 남겼거나, 읽기 상태(UserBookStatus)를 설정한 도서를 모두
    // 대상으로 한다(PM 확인: 상태만 설정하고 흔적은 없는 책도 노출). 항목은 도서가 아니라 (도서, 모임) 단위다
    // (이슈 #177): 모임 전용 대목은 groupId를 붙여야만 조회되므로, 전역 대목에서의 활동과 모임 대목에서의
    // 활동을 한 항목으로 합치면 프론트가 어느 화면으로 보내야 할지 알 수 없다. 모임 항목은 지금 내가 멤버인
    // 모임만 포함한다(멤버가 아니면 들어가도 조회 권한이 없다).
    // 대목 수/흔적 수는 그 항목의 범위(전역이면 group_id IS NULL, 모임이면 그 모임) 안에서만 집계하고, 흔적 수는
    // opinionCountScope에 따라 범위 전체(ALL, 홈 노출용) 또는 로그인 사용자 본인이 남긴 것(MINE, 마이페이지
    // 노출용)만 센다. 정렬 기준은 "이 항목에 대한 내 최근 활동 시각"이며, 흔적을 남긴 시각(opinion.createdAt)과
    // 읽기 상태를 설정/갱신한 시각(userBookStatus.updatedAt) 중 더 나중 것을 쓴다. 두 활동의 시각 컬럼이 서로
    // 다른 테이블에 있어 하나의 JOIN/GROUP BY로 합쳐 정렬하기 까다로우므로, 두 후보 집합을 각각 조회해
    // Java에서 병합·정렬·페이지네이션한 뒤 해당 페이지의 항목만 집계 쿼리로 채운다.
    public Page<MyLibraryBookProjection> findMyLibraryBooks(
            Long userId, Pageable pageable, OpinionCountScope opinionCountScope) {
        Map<LibraryEntryKey, LocalDateTime> lastActivityByEntry = new LinkedHashMap<>();

        QOpinion opinionMine = QOpinion.opinion;
        QPassage passageForActivity = QPassage.passage;
        for (Tuple row : queryFactory
                .select(passageForActivity.book.id, passageForActivity.group.id, opinionMine.createdAt.max())
                .from(opinionMine)
                .join(opinionMine.passage, passageForActivity)
                .where(opinionMine.user.id.eq(userId), opinionMine.deletedAt.isNull(),
                        passageForActivity.deletedAt.isNull(), globalOrMyGroup(passageForActivity, userId))
                .groupBy(passageForActivity.book.id, passageForActivity.group.id)
                .fetch()) {
            lastActivityByEntry.put(
                    new LibraryEntryKey(row.get(passageForActivity.book.id), row.get(passageForActivity.group.id)),
                    row.get(opinionMine.createdAt.max()));
        }

        QUserBookStatus userBookStatus = QUserBookStatus.userBookStatus;
        for (Tuple row : queryFactory
                .select(userBookStatus.book.id, userBookStatus.updatedAt)
                .from(userBookStatus)
                .where(userBookStatus.user.id.eq(userId))
                .fetch()) {
            LibraryEntryKey key = statusEntryKey(lastActivityByEntry, row.get(userBookStatus.book.id));
            lastActivityByEntry.merge(key, row.get(userBookStatus.updatedAt), BookQueryRepository::later);
        }

        List<LibraryEntryKey> sortedEntries = lastActivityByEntry.entrySet().stream()
                .sorted(Map.Entry.<LibraryEntryKey, LocalDateTime>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey, LibraryEntryKey.TIE_BREAKER))
                .map(Map.Entry::getKey)
                .toList();

        List<LibraryEntryKey> pageEntries = sortedEntries.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();

        List<MyLibraryBookProjection> content = fetchLibraryEntries(pageEntries, userId, opinionCountScope);
        return new PageImpl<>(content, pageable, sortedEntries.size());
    }

    // 읽기 상태는 (도서, 모임)이 아니라 도서 단위라 어느 항목에 반영할지 정해야 한다. 전역 항목이 있거나
    // 그 도서에 아무 항목도 없으면 전역 항목에 반영하고, 모임 항목만 있으면 그중 가장 최근 항목에 반영한다
    // (그러지 않으면 모임에서만 읽는 책인데 대목 0개짜리 전역 항목이 하나 더 생긴다).
    private LibraryEntryKey statusEntryKey(Map<LibraryEntryKey, LocalDateTime> lastActivityByEntry, Long bookId) {
        LibraryEntryKey globalKey = new LibraryEntryKey(bookId, null);
        if (lastActivityByEntry.containsKey(globalKey)) {
            return globalKey;
        }
        return lastActivityByEntry.entrySet().stream()
                .filter(entry -> entry.getKey().bookId().equals(bookId))
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(globalKey);
    }

    private static LocalDateTime later(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    // 전역 공개 대목이거나, 내가 지금 멤버인 모임의 대목
    private BooleanExpression globalOrMyGroup(QPassage passage, Long userId) {
        QGroupMember groupMember = QGroupMember.groupMember;
        return passage.group.isNull().or(JPAExpressions.selectOne()
                .from(groupMember)
                .where(groupMember.group.id.eq(passage.group.id), groupMember.user.id.eq(userId))
                .exists());
    }

    // 비로그인 홈의 샘플 도서 카드 집계용. 그 도서에 다른 사용자들이 남긴 대목/흔적이 섞여 있어도(dev처럼)
    // 샘플 계정(GuestSampleAccount)이 씨딩한 것만 세어, 카드의 숫자가 실제 샘플 미리보기 내용과 일치하게 한다.
    public long countSamplePassages(Long bookId) {
        QPassage passage = QPassage.passage;
        Long count = queryFactory
                .select(passage.count())
                .from(passage)
                .where(passage.book.id.eq(bookId), passage.deletedAt.isNull(),
                        passage.creator.snsProvider.eq(GuestSampleAccount.SNS_PROVIDER),
                        passage.creator.snsId.eq(GuestSampleAccount.SNS_ID))
                .fetchOne();
        return count == null ? 0L : count;
    }

    public long countSampleOpinions(Long bookId) {
        QOpinion opinion = QOpinion.opinion;
        Long count = queryFactory
                .select(opinion.count())
                .from(opinion)
                .where(opinion.passage.book.id.eq(bookId), opinion.deletedAt.isNull(),
                        opinion.passage.deletedAt.isNull(),
                        opinion.user.snsProvider.eq(GuestSampleAccount.SNS_PROVIDER),
                        opinion.user.snsId.eq(GuestSampleAccount.SNS_ID))
                .fetchOne();
        return count == null ? 0L : count;
    }

    private List<MyLibraryBookProjection> fetchLibraryEntries(
            List<LibraryEntryKey> entries, Long userId, OpinionCountScope opinionCountScope) {
        if (entries.isEmpty()) {
            return List.of();
        }

        QBook book = QBook.book;
        QGroup group = QGroup.group;
        QPassage passage = QPassage.passage;
        QOpinion opinion = QOpinion.opinion;

        List<Long> bookIds = entries.stream().map(LibraryEntryKey::bookId).distinct().toList();
        List<Long> groupIds = entries.stream().map(LibraryEntryKey::groupId).filter(Objects::nonNull).distinct().toList();

        Map<Long, Book> booksById = queryFactory.selectFrom(book).where(book.id.in(bookIds)).fetch().stream()
                .collect(Collectors.toMap(Book::getId, b -> b));
        Map<Long, String> groupNamesById = groupIds.isEmpty() ? Map.of() : queryFactory
                .select(group.id, group.name)
                .from(group)
                .where(group.id.in(groupIds))
                .fetch().stream()
                .collect(Collectors.toMap(row -> row.get(group.id), row -> row.get(group.name)));

        // 대목 수/흔적 수를 (도서, 모임) 단위로 한 번에 집계한다. passage.group.id는 FK 컬럼을 그대로 쓰므로
        // (group 테이블 조인 없음) 전역 대목(group_id IS NULL)도 하나의 그룹으로 묶인다. 페이지에 없는 다른
        // 모임의 집계도 함께 나오지만 아래에서 페이지 항목만 골라 쓴다.
        BooleanExpression opinionOnPassage = opinion.passage.eq(passage).and(opinion.deletedAt.isNull());
        if (opinionCountScope == OpinionCountScope.MINE) {
            opinionOnPassage = opinionOnPassage.and(opinion.user.id.eq(userId));
        }
        Map<LibraryEntryKey, EntryCounts> countsByEntry = queryFactory
                .select(passage.book.id, passage.group.id, passage.countDistinct(), opinion.countDistinct())
                .from(passage)
                .leftJoin(opinion).on(opinionOnPassage)
                .where(passage.book.id.in(bookIds), passage.deletedAt.isNull())
                .groupBy(passage.book.id, passage.group.id)
                .fetch().stream()
                .collect(Collectors.toMap(
                        row -> new LibraryEntryKey(row.get(passage.book.id), row.get(passage.group.id)),
                        row -> new EntryCounts(row.get(passage.countDistinct()), row.get(opinion.countDistinct()))));

        return entries.stream()
                .filter(entry -> booksById.containsKey(entry.bookId()))
                .map(entry -> {
                    Book entryBook = booksById.get(entry.bookId());
                    EntryCounts counts = countsByEntry.getOrDefault(entry, EntryCounts.EMPTY);
                    return new MyLibraryBookProjection(
                            entryBook.getId(), entryBook.getTitle(), entryBook.getAuthor(), entryBook.getPublisher(),
                            entryBook.getCoverImageUrl(), entry.groupId(),
                            entry.groupId() != null ? groupNamesById.get(entry.groupId()) : null,
                            counts.passageCount(), counts.opinionCount());
                })
                .toList();
    }

    // 내 서재 항목의 식별자. groupId가 null이면 전역 공개 항목이다.
    private record LibraryEntryKey(Long bookId, Long groupId) {

        // 마지막 활동 시각이 같을 때의 정렬: 도서 ID 내림차순(기존 동작 유지), 같은 도서면 전역 항목이 먼저.
        static final Comparator<LibraryEntryKey> TIE_BREAKER = Comparator
                .comparing(LibraryEntryKey::bookId, Comparator.reverseOrder())
                .thenComparing(LibraryEntryKey::groupId, Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    // 읽기 상태만 설정한 도서처럼 대목이 하나도 없는 항목은 집계 결과에 없으므로 0으로 채운다.
    private record EntryCounts(long passageCount, long opinionCount) {

        static final EntryCounts EMPTY = new EntryCounts(0L, 0L);
    }

    public Page<BookActivityProjection> findPopularBooks(Pageable pageable) {
        QBook book = QBook.book;
        QPassage passage = QPassage.passage;
        QOpinion opinion = QOpinion.opinion;

        List<BookActivityProjection> content = queryFactory
                .select(Projections.constructor(BookActivityProjection.class,
                        book.id, book.title, book.author, book.publisher, book.coverImageUrl,
                        passage.countDistinct(), opinion.countDistinct()))
                .from(book)
                .innerJoin(passage).on(passage.book.eq(book).and(passage.deletedAt.isNull()))
                .leftJoin(opinion).on(opinion.passage.eq(passage).and(opinion.deletedAt.isNull()))
                .groupBy(book.id, book.title, book.author, book.publisher, book.coverImageUrl)
                .orderBy(opinion.countDistinct().desc(), book.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, countBooksWithPassages());
    }

    // 도서 상세는 목록 API와 달리 대목이 하나도 없는 도서도 조회 대상이므로(단건 조회 후 404 판별 용도),
    // passage/opinion 모두 leftJoin으로 집계해 존재하지 않으면 0으로 내려준다.
    public Optional<BookDetailProjection> findBookDetail(Long bookId) {
        QBook book = QBook.book;
        QPassage passage = QPassage.passage;
        QOpinion opinion = QOpinion.opinion;

        BookDetailProjection result = queryFactory
                .select(Projections.constructor(BookDetailProjection.class,
                        book.id, book.title, book.author, book.publisher, book.pageCount, book.coverImageUrl,
                        passage.countDistinct(), opinion.countDistinct()))
                .from(book)
                .leftJoin(passage).on(passage.book.eq(book).and(passage.deletedAt.isNull()))
                .leftJoin(opinion).on(opinion.passage.eq(passage).and(opinion.deletedAt.isNull()))
                .where(book.id.eq(bookId))
                .groupBy(book.id, book.title, book.author, book.publisher, book.pageCount, book.coverImageUrl)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    // "최근에 남긴 책"은 Passage를 새로 만든 책이 아니라 흔적(Opinion)을 남긴 책 기준이다 (FR-WRITE-01).
    // 기존 Passage에 Opinion만 추가한 경우도 포함해야 하므로 Opinion을 기준으로 조회한다.
    // keyword가 있으면 홈 화면 검색용으로 제목을 추가 필터링한다 (searchByTitle과 동일하게
    // title_normalized 컬럼 기준). keyword가 없거나 빈 문자열이면 필터링 없이 전체를 반환한다.
    public Page<Long> findRecentlyActiveBookIds(Long userId, String keyword, Pageable pageable) {
        QOpinion opinion = QOpinion.opinion;
        QPassage passage = QPassage.passage;
        QBook book = QBook.book;

        String normalizedKeyword = Book.normalize(keyword);
        boolean hasKeyword = !normalizedKeyword.isEmpty();

        List<Long> content = queryFactory
                .select(passage.book.id)
                .from(opinion)
                .join(opinion.passage, passage)
                .join(passage.book, book)
                .where(opinion.user.id.eq(userId), opinion.deletedAt.isNull(),
                        hasKeyword ? book.titleNormalized.contains(normalizedKeyword) : null)
                .groupBy(passage.book.id)
                .orderBy(opinion.createdAt.max().desc(), passage.book.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(passage.book.countDistinct())
                .from(opinion)
                .join(opinion.passage, passage)
                .join(passage.book, book)
                .where(opinion.user.id.eq(userId), opinion.deletedAt.isNull(),
                        hasKeyword ? book.titleNormalized.contains(normalizedKeyword) : null)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private long countBooksWithPassages() {
        QBook book = QBook.book;
        QPassage passage = QPassage.passage;

        Long count = queryFactory
                .select(book.countDistinct())
                .from(book)
                .innerJoin(passage).on(passage.book.eq(book).and(passage.deletedAt.isNull()))
                .fetchOne();
        return count != null ? count : 0L;
    }
}
