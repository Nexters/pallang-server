package com.nexters.palang.domain.opinion.application;

import com.nexters.palang.domain.book.application.BookOptionProjection;
import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.decoration.domain.Decoration;
import com.nexters.palang.domain.group.application.GroupAccessValidator;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.opinion.common.error.OpinionErrorCode;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.domain.OpinionSortType;
import com.nexters.palang.domain.opinion.domain.event.OpinionCreatedEvent;
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
import com.nexters.palang.domain.user.domain.GuestSampleAccount;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private final ApplicationEventPublisher eventPublisher;
    private final GroupRepository groupRepository;
    private final GroupAccessValidator groupAccessValidator;

    // 흔적 목록(FR-OPINION-03): 최신순(기본)/좋아요순. currentUserId는 비로그인 시 null(liked는 항상 false).
    // 대목이 모임 전용이면(passage.group != null) 모임원만 조회할 수 있다.
    public Page<OpinionSummaryProjection> getOpinions(
            Long passageId, OpinionSortType sortType, Pageable pageable, Long currentUserId) {
        Passage passage = passageRepository.findByIdAndDeletedAtIsNull(passageId)
                .orElseThrow(() -> new PassageException(PassageErrorCode.PASSAGE_NOT_FOUND));
        if (passage.getGroup() != null) {
            groupAccessValidator.validateMember(passage.getGroup().getId(), currentUserId);
        }
        return opinionQueryRepository.findOpinions(passageId, sortType, pageable, currentUserId);
    }

    // 흔적 상세(FR-OPINION-05): 이 흔적 작성자가 기록한 꾸밈을 그대로 보여준다.
    // 대목이 모임 전용이면 모임원만 조회할 수 있다. currentUserId는 비로그인 시 null(Soft Authentication).
    public Opinion getOpinion(Long opinionId, Long currentUserId) {
        Opinion opinion = getExistingOpinion(opinionId);
        Group group = opinion.getPassage().getGroup();
        if (group != null) {
            groupAccessValidator.validateMember(group.getId(), currentUserId);
        }
        return opinion;
    }

    // 모임 전용 흔적이어도 별도 group 검증은 하지 않는다: 수정/삭제는 작성자 본인만 가능하고(validateOwner),
    // 이 앱에는 모임 나가기/강퇴가 없어 한 번 모임원이면 계속 모임원이다 — 즉 작성 시점에 이미 모임원임이
    // 확인됐던 사람이라면 지금도 여전히 모임원이라는 뜻이라 owner 검증만으로 충분하다. 나가기/강퇴가 생기면
    // 이 가정이 깨지므로 그때는 group 검증을 함께 추가해야 한다.
    @Transactional
    public Opinion modifyOpinion(Long opinionId, Long userId, UpdateOpinionRequest request) {
        Opinion opinion = getExistingOpinion(opinionId);
        validateOwner(opinion, userId);
        opinion.updateContent(request.content());
        return opinion;
    }

    // 대목은 여러 사용자가 공유하는 단위이므로, 이 삭제로 그 대목에 살아있는 흔적이 하나도 남지 않을 때만
    // 대목도 함께 소프트 삭제한다 (PM 요구사항: 흔적 0개인 대목은 존재할 수 없다).
    // group 검증 불필요 이유는 modifyOpinion 주석 참고.
    @Transactional
    public void removeOpinion(Long opinionId, Long userId) {
        Opinion opinion = getExistingOpinion(opinionId);
        validateOwner(opinion, userId);
        opinion.delete();

        Long passageId = opinion.getPassage().getId();
        if (!opinionRepository.existsByPassageIdAndDeletedAtIsNullAndIdNot(passageId, opinion.getId())) {
            opinion.getPassage().delete();
        }
    }

    private Opinion getExistingOpinion(Long opinionId) {
        Opinion opinion = opinionRepository.findDetailById(opinionId)
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
    // request.groupId()가 있으면 그 모임 전용 흔적/대목이 되며, 작성자가 모임원인지 먼저 검증한다.
    // 존재하지 않는 groupId는 (멤버십 검증이 항상 false로 실패해 403을 내기 전에) 여기서 먼저 404로 걸러낸다.
    @Transactional
    public Opinion createOpinion(Long userId, CreateOpinionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        Group group = resolveGroup(request.groupId());
        if (group != null) {
            groupAccessValidator.validateMember(group.getId(), userId);
        }

        boolean merged = request.passageId() != null;
        Passage passage = merged ? mergeIntoExistingPassage(request) : createNewPassage(request, user, group);

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
        eventPublisher.publishEvent(new OpinionCreatedEvent(opinion.getId(), passage.getBook().getId(), userId));
        return opinion;
    }

    private Passage mergeIntoExistingPassage(CreateOpinionRequest request) {
        Passage existing = passageRepository.findById(request.passageId())
                .orElseThrow(() -> new PassageException(PassageErrorCode.PASSAGE_NOT_FOUND));
        if (existing.isDeleted()) {
            throw new PassageException(PassageErrorCode.PASSAGE_NOT_FOUND);
        }
        if (!existing.getBook().getId().equals(request.bookId())) {
            throw new PassageException(PassageErrorCode.PASSAGE_BOOK_MISMATCH);
        }
        // 병합 대상 대목의 소속 모임과 요청이 지정한 모임이 정확히 일치해야 한다(둘 다 null=전역 공개도 일치).
        // 그렇지 않으면 전역 공개 대목에 모임 흔적이 섞이거나 그 반대가 될 수 있다.
        Long existingGroupId = existing.getGroup() != null ? existing.getGroup().getId() : null;
        if (!Objects.equals(existingGroupId, request.groupId())) {
            throw new PassageException(PassageErrorCode.PASSAGE_GROUP_MISMATCH);
        }
        return existing;
    }

    // 비로그인 사용자와, 로그인했지만 남긴 흔적이 (지금 시점 기준으로) 하나도 없는 계정은 마이페이지
    // 미리보기로 샘플 계정(GuestSampleAccount)의 실제 흔적을 본다 (기획 확정, 이슈 #120). 이 계정과 그
    // 계정의 흔적/꾸밈은 OpinionGuestSampleSeedRunner가 앱 기동 시 미리 만들어둔다 — 하드코딩 대신
    // 실데이터를 재사용해야 상세 화면의 Decoration(밑줄/동그라미 등)도 자연스럽게 함께 보인다. 씨딩 전이거나
    // 실패한 환경(로컬 등)에서는 빈 목록을 반환한다. bookId로 필터링해서 우연히 결과가 0건인 경우(다른
    // 책에는 흔적이 있음)와 구분하기 위해, "흔적이 하나도 없다"는 판단은 bookId 필터 없이 전체 기준으로 한다.
    public Page<MyOpinionProjection> getMyOpinions(Long userId, Long bookId, Pageable pageable) {
        boolean hasNoRealOpinions =
                userId != null && opinionRepository.countByUserIdAndDeletedAtIsNull(userId) == 0;
        if (userId == null || hasNoRealOpinions) {
            return userRepository
                    .findBySnsProviderAndSnsId(GuestSampleAccount.SNS_PROVIDER, GuestSampleAccount.SNS_ID)
                    .map(sampleUser -> opinionQueryRepository.findMyOpinions(sampleUser.getId(), bookId, pageable))
                    .orElseGet(() -> new PageImpl<>(List.of(), pageable, 0));
        }
        return opinionQueryRepository.findMyOpinions(userId, bookId, pageable);
    }

    public Page<LikedOpinionProjection> getLikedOpinions(Long userId, Long bookId, Pageable pageable) {
        return opinionQueryRepository.findLikedOpinions(userId, bookId, pageable);
    }

    // 좋아요 관리 화면의 "전체 책 보기" 드롭다운: 내가 좋아요를 누른 흔적이 있는 도서 목록.
    public Page<BookOptionProjection> getLikedBookOptions(Long userId, Pageable pageable) {
        return opinionQueryRepository.findLikedBookOptions(userId, pageable);
    }

    public long getMyOpinionCount(Long userId) {
        return opinionRepository.countByUserIdAndDeletedAtIsNull(userId);
    }

    // 모임은 생성 시 책이 고정되므로(Group 불변식), 이 모임 소속으로 새 대목을 만들 때 요청의 bookId가
    // 그 모임의 책과 다르면 막는다.
    private Passage createNewPassage(CreateOpinionRequest request, User creator, Group group) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));
        if (group != null && !group.getBook().getId().equals(request.bookId())) {
            throw new GroupException(GroupErrorCode.GROUP_BOOK_MISMATCH);
        }
        Passage passage = Passage.builder()
                .book(book)
                .creator(creator)
                .group(group)
                .pageNumber(request.pageNumber())
                .quotedText(request.quotedText())
                .isSpoiler(request.isSpoiler())
                .normalizedHash(PassageNormalizer.normalizedHash(request.quotedText()))
                .build();
        return passageRepository.save(passage);
    }

    private Group resolveGroup(Long groupId) {
        if (groupId == null) {
            return null;
        }
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
    }
}
