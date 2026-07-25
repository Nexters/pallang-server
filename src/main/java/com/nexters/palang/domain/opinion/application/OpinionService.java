package com.nexters.palang.domain.opinion.application;

import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.decoration.domain.Decoration;
import com.nexters.palang.domain.opinion.common.error.OpinionErrorCode;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.domain.OpinionSortType;
import com.nexters.palang.domain.opinion.infrastructure.OpinionQueryRepository;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.opinion.presentation.dto.CreateOpinionRequest;
import com.nexters.palang.domain.opinion.presentation.dto.UpdateOpinionRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OpinionService {

    private final OpinionRepository opinionRepository;
    private final OpinionQueryRepository opinionQueryRepository;
    private final PassageRepository passageRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    // 흔적 목록(FR-OPINION-03): 최신순(기본)/좋아요순.
    public Page<OpinionSummaryProjection> getOpinions(Long passageId, OpinionSortType sortType, Pageable pageable) {
        if (!passageRepository.existsById(passageId)) {
            throw new PassageException(PassageErrorCode.PASSAGE_NOT_FOUND);
        }
        return opinionQueryRepository.findOpinions(passageId, sortType, pageable);
    }

    // 흔적 상세(FR-OPINION-05): 이 흔적 작성자가 기록한 꾸밈을 그대로 보여준다.
    public Opinion getOpinion(Long opinionId) {
        return getExistingOpinion(opinionId);
    }

    @Transactional
    public Opinion modifyOpinion(Long opinionId, Long userId, UpdateOpinionRequest request) {
        Opinion opinion = getExistingOpinion(opinionId);
        validateOwner(opinion, userId);
        opinion.updateContent(request.content());
        return opinion;
    }

    @Transactional
    public void removeOpinion(Long opinionId, Long userId) {
        Opinion opinion = getExistingOpinion(opinionId);
        validateOwner(opinion, userId);
        opinion.delete();
    }

    private Opinion getExistingOpinion(Long opinionId) {
        Opinion opinion = opinionRepository.findById(opinionId)
                .orElseThrow(() -> new OpinionException(OpinionErrorCode.OPINION_NOT_FOUND));
        if (opinion.isDeleted()) {
            throw new OpinionException(OpinionErrorCode.OPINION_NOT_FOUND);
        }
        return opinion;
    }

    private void validateOwner(Opinion opinion, Long userId) {
        if (!opinion.getUser().getId().equals(userId)) {
            throw new OpinionException(OpinionErrorCode.OPINION_FORBIDDEN);
        }
    }

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
