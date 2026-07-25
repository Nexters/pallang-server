package com.nexters.palang.domain.opinion.application;

import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.decoration.domain.Decoration;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.opinion.presentation.dto.CreateOpinionRequest;
import com.nexters.palang.domain.passage.application.PassageNormalizer;
import com.nexters.palang.domain.passage.common.error.PassageErrorCode;
import com.nexters.palang.domain.passage.common.error.PassageException;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import com.nexters.palang.domain.user.common.error.UserErrorCode;
import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OpinionService {

    private final OpinionRepository opinionRepository;
    private final PassageRepository passageRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    // Passage(신규 생성 또는 기존 병합) + Opinion + Decoration을 한 트랜잭션에서 원자적으로 생성한다. (직접 입력만)
    @Transactional
    public Opinion createOpinion(Long userId, CreateOpinionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        boolean merged = request.passageId() != null;
        Passage passage = merged ? mergeIntoExistingPassage(request) : createNewPassage(request, user);

        List<Decoration> decorations = request.decorations().stream()
                .map(d -> Decoration.builder()
                        .startOffset(d.startOffset())
                        .endOffset(d.endOffset())
                        .effectType(d.effectType())
                        .color(d.color())
                        .build())
                .toList();

        Opinion opinion = Opinion.createWithDecorations(passage, user, request.content(), decorations);
        opinionRepository.save(opinion);
        return opinion;
    }

    private Passage mergeIntoExistingPassage(CreateOpinionRequest request) {
        Passage existing = passageRepository.findById(request.passageId())
                .orElseThrow(() -> new PassageException(PassageErrorCode.PASSAGE_NOT_FOUND));
        if (!existing.getBook().getId().equals(request.bookId())) {
            throw new PassageException(PassageErrorCode.PASSAGE_BOOK_MISMATCH);
        }
        return existing;
    }

    private Passage createNewPassage(CreateOpinionRequest request, User creator) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));
        Passage passage = Passage.builder()
                .book(book)
                .creator(creator)
                .pageNumber(request.pageNumber())
                .quotedText(request.quotedText())
                .isSpoiler(request.isSpoiler())
                .normalizedHash(PassageNormalizer.normalizedHash(request.quotedText()))
                .build();
        return passageRepository.save(passage);
    }
}
