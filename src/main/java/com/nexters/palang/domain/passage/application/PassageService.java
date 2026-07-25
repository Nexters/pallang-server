package com.nexters.palang.domain.passage.application;

import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import com.nexters.palang.domain.passage.presentation.request.PassageRequest;
import com.nexters.palang.domain.user.common.error.UserErrorCode;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.user.common.error.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PassageService {

    private final PassageRepository passageRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    // 특정 도서(Book)에 대한 대목(Passage) 등록
    @Transactional
    public Passage addPassage(PassageRequest.Create request) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));
        User creator = userRepository.findById(request.creatorId())
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        Passage passage = Passage.builder()
                .book(book)
                .creator(creator)
                .pageNumber(request.pageNumber())
                .quotedText(request.quotedText())
                .isSpoiler(request.isSpoiler())
                .build();

        return passageRepository.save(passage);
    }
}
