package com.nexters.palang.domain.book.application;

import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.UserBookStatus;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.book.infrastructure.UserBookStatusRepository;
import com.nexters.palang.domain.book.presentation.dto.UpdateUserBookStatusRequest;
import com.nexters.palang.domain.user.common.error.UserErrorCode;
import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBookStatusService {

    private final UserBookStatusRepository userBookStatusRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    // 유저-도서 읽기상태가 없으면 새로 만들고, 있으면 갱신한다. (FR-WRITE-08)
    @Transactional
    public UserBookStatus updateBookStatus(Long userId, UpdateUserBookStatusRequest request) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));
        validateCurrentPage(request.currentPage(), book);

        return userBookStatusRepository.findByUserIdAndBookId(userId, request.bookId())
                .map(existing -> updateExisting(existing, request))
                .orElseGet(() -> createNew(userId, book, request));
    }

    private UserBookStatus updateExisting(UserBookStatus existing, UpdateUserBookStatusRequest request) {
        existing.updateStatus(request.status());
        existing.updateCurrentPage(request.currentPage());
        return existing;
    }

    private UserBookStatus createNew(Long userId, Book book, UpdateUserBookStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        UserBookStatus userBookStatus = UserBookStatus.builder()
                .user(user)
                .book(book)
                .status(request.status())
                .currentPage(request.currentPage())
                .build();
        return userBookStatusRepository.save(userBookStatus);
    }

    private void validateCurrentPage(Integer currentPage, Book book) {
        if (currentPage != null && currentPage > book.getPageCount()) {
            throw new BookException(BookErrorCode.INVALID_CURRENT_PAGE);
        }
    }
}
