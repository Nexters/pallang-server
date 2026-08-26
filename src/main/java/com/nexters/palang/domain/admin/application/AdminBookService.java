package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.book.infrastructure.UserBookStatusRepository;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 페이지에서 책(Book)을 조회/수정/삭제하기 위한 서비스(issue #155 후속). BookService에는 애초에
// 도서 수정/삭제 기능이 없다(등록만 가능). 책 삭제는 그 책을 쓰는 여러 유저의 모임/대목까지 통째로 지울 수
// 있는 위험한 작업이라는 점을 감안해, 연관된 모임/대목/의견/댓글까지 전부 함께 정리한다(issue #155 참고
// 사항: "모두 cascade 삭제"로 결정).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBookService {

    private final BookRepository bookRepository;
    private final PassageRepository passageRepository;
    private final GroupRepository groupRepository;
    private final UserBookStatusRepository userBookStatusRepository;
    private final AdminCascadeDeleter cascadeDeleter;

    public Page<Book> searchBooks(String keyword, Pageable pageable) {
        return bookRepository.findByTitleContainingOrAuthorContaining(keyword, keyword, pageable);
    }

    @Transactional
    public Book updateBook(
            Long bookId, String title, String author, String publisher, int pageCount, String isbn, String coverImageUrl) {
        Book book = getExistingBook(bookId);
        book.update(title, author, publisher, pageCount, isbn, coverImageUrl);
        return book;
    }

    // 이 책으로 만들어진 모임(과 그 안의 대목/의견/댓글/데코/좋아요, 모임원), 모임에 속하지 않은 대목까지
    // 전부 하드 삭제한 뒤 책 자체를 지운다. 여러 유저의 데이터에 영향을 줄 수 있는 위험한 작업이다.
    @Transactional
    public void deleteBook(Long bookId) {
        getExistingBook(bookId);

        List<Long> groupIds = groupRepository.findAllByBookId(bookId).stream().map(Group::getId).toList();
        cascadeDeleter.deleteGroups(groupIds);

        // 모임에 속하지 않은(전역 공개) 대목을 포함해, 이 책에 남아있는 대목을 전부 정리한다.
        // 모임 소속 대목은 위 deleteGroups에서 이미 지워졌으므로 findAllByBookId는 나머지만 돌려준다.
        List<Long> remainingPassageIds = passageRepository.findAllByBookId(bookId).stream().map(Passage::getId).toList();
        cascadeDeleter.deletePassages(remainingPassageIds);

        userBookStatusRepository.deleteAllByBookId(bookId);
        bookRepository.deleteById(bookId);
    }

    private Book getExistingBook(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));
    }
}
