package com.nexters.palang.domain.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.admin.common.error.AdminException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.notification.application.NotificationCreationService;
import com.nexters.palang.domain.notification.domain.RemovalReason;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import com.nexters.palang.domain.report.domain.ReportTargetType;
import com.nexters.palang.domain.report.infrastructure.ReportRepository;
import com.nexters.palang.domain.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminOpinionServiceTest {

    @Mock
    private OpinionRepository opinionRepository;
    @Mock
    private PassageRepository passageRepository;
    @Mock
    private AdminCascadeDeleter cascadeDeleter;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private NotificationCreationService notificationCreationService;

    private AdminOpinionService adminOpinionService;

    @BeforeEach
    void setUp() {
        adminOpinionService = new AdminOpinionService(
                opinionRepository, passageRepository, cascadeDeleter, reportRepository, notificationCreationService);
    }

    private static final LocalDateTime WRITTEN_AT = LocalDateTime.of(2026, 10, 19, 14, 30);

    // 사유 선택 삭제에 필요한 연관(작성자/대목/책/모임)을 갖춘 의견. 그 대목에 다른 의견이 남아 있는 상황으로 둔다.
    private Opinion opinionForRemoval(Group group) {
        Book book = mock(Book.class);
        given(book.getTitle()).willReturn("책 제목");
        Passage passage = mock(Passage.class);
        given(passage.getId()).willReturn(100L);
        given(passage.getGroup()).willReturn(group);
        given(passage.getBook()).willReturn(book);
        lenient().when(passage.getQuotedText()).thenReturn("발췌란에 쓴 의견");
        User author = mock(User.class);
        given(author.getId()).willReturn(7L);
        Opinion opinion = mock(Opinion.class);
        given(opinion.getPassage()).willReturn(passage);
        given(opinion.getUser()).willReturn(author);
        given(opinion.getCreatedAt()).willReturn(WRITTEN_AT);
        lenient().when(opinion.getContent()).thenReturn("광고 문구");
        given(opinionRepository.findWithAssociationsById(1L)).willReturn(Optional.of(opinion));
        lenient().when(opinionRepository.existsByPassageIdAndDeletedAtIsNullAndIdNot(100L, 1L))
                .thenReturn(true);
        return opinion;
    }

    @Test
    @DisplayName("사유 선택 삭제: 전역 게시물은 신고 없이도 삭제되고, 작성자에게 식별 정보와 함께 운영 안내가 간다")
    void removeOpinionWithReasonDeletesAndNotifiesAuthor() {
        opinionForRemoval(null);

        adminOpinionService.removeOpinionWithReason(1L, RemovalReason.ADVERTISEMENT);

        verify(cascadeDeleter).deleteOpinions(List.of(1L));
        verify(notificationCreationService).createContentRemovedNotice(
                7L, RemovalReason.ADVERTISEMENT, "책 제목", WRITTEN_AT, "광고 문구");
    }

    @Test
    @DisplayName("사유 선택 삭제: '발췌란에 의견 작성' 사유는 발췌문 내용을 안내에 담는다")
    void removeOpinionWithReasonUsesQuotedTextForPassageMisuse() {
        opinionForRemoval(null);

        adminOpinionService.removeOpinionWithReason(1L, RemovalReason.PASSAGE_FIELD_MISUSE);

        verify(notificationCreationService).createContentRemovedNotice(
                7L, RemovalReason.PASSAGE_FIELD_MISUSE, "책 제목", WRITTEN_AT, "발췌란에 쓴 의견");
    }

    @Test
    @DisplayName("사유 선택 삭제: 신고가 없는 모임 게시물은 삭제할 수 없다")
    void removeOpinionWithReasonRejectsUnreportedGroupContent() {
        Group group = mock(Group.class);
        Opinion opinion = mock(Opinion.class);
        Passage passage = mock(Passage.class);
        given(passage.getGroup()).willReturn(group);
        given(opinion.getPassage()).willReturn(passage);
        given(opinionRepository.findWithAssociationsById(1L)).willReturn(Optional.of(opinion));
        given(reportRepository.countByTargetTypeAndTargetId(ReportTargetType.OPINION, 1L)).willReturn(0L);

        assertThatThrownBy(() -> adminOpinionService.removeOpinionWithReason(1L, RemovalReason.ABUSE))
                .isInstanceOf(AdminException.class);
        verify(cascadeDeleter, never()).deleteOpinions(any());
        verify(notificationCreationService, never()).createContentRemovedNotice(
                any(), any(), any(),
                any(), any());
    }

    @Test
    @DisplayName("사유 선택 삭제: 신고가 접수된 모임 게시물은 삭제할 수 있다")
    void removeOpinionWithReasonAllowsReportedGroupContent() {
        opinionForRemoval(mock(Group.class));
        given(reportRepository.countByTargetTypeAndTargetId(ReportTargetType.OPINION, 1L)).willReturn(2L);

        adminOpinionService.removeOpinionWithReason(1L, RemovalReason.ABUSE);

        verify(cascadeDeleter).deleteOpinions(List.of(1L));
        verify(notificationCreationService).createContentRemovedNotice(
                7L, RemovalReason.ABUSE, "책 제목", WRITTEN_AT, "광고 문구");
    }

    @Test
    @DisplayName("대목 id로 조회하면 그 대목의 의견을 작성 순서대로 가져오도록 리포지토리에 위임한다")
    void searchOpinionsByPassageDelegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Opinion opinion = mock(Opinion.class);
        Page<Opinion> expected = new PageImpl<>(List.of(opinion), pageable, 1);
        given(opinionRepository.findByPassageIdOrderByCreatedAtAscIdAsc(100L, pageable)).willReturn(expected);

        Page<Opinion> results = adminOpinionService.searchOpinionsByPassage(100L, pageable);

        assertThat(results).isEqualTo(expected);
    }

    @Test
    @DisplayName("존재하지 않는 의견을 삭제하려 하면 예외가 발생한다")
    void deleteFailsWhenOpinionNotFound() {
        given(opinionRepository.findWithAssociationsById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminOpinionService.deleteOpinion(1L)).isInstanceOf(OpinionException.class);
    }

    @Test
    @DisplayName("대목에 남은 다른 의견이 없으면 의견 삭제 시 대목도 함께 소프트 삭제된다")
    void deleteOpinionSoftDeletesPassageWhenLast() {
        Passage passage = mock(Passage.class);
        given(passage.getId()).willReturn(100L);
        Opinion opinion = mock(Opinion.class);
        given(opinion.getPassage()).willReturn(passage);
        given(opinionRepository.findWithAssociationsById(1L)).willReturn(Optional.of(opinion));
        given(opinionRepository.existsByPassageIdAndDeletedAtIsNullAndIdNot(100L, 1L)).willReturn(false);
        given(passageRepository.findById(100L)).willReturn(Optional.of(passage));

        adminOpinionService.deleteOpinion(1L);

        verify(cascadeDeleter).deleteOpinions(List.of(1L));
        verify(passage).delete();
    }

    @Test
    @DisplayName("대목에 다른 살아있는 의견이 남아있으면 대목은 삭제되지 않는다")
    void deleteOpinionKeepsPassageWhenOthersRemain() {
        Passage passage = mock(Passage.class);
        given(passage.getId()).willReturn(100L);
        Opinion opinion = mock(Opinion.class);
        given(opinion.getPassage()).willReturn(passage);
        given(opinionRepository.findWithAssociationsById(1L)).willReturn(Optional.of(opinion));
        given(opinionRepository.existsByPassageIdAndDeletedAtIsNullAndIdNot(100L, 1L)).willReturn(true);

        adminOpinionService.deleteOpinion(1L);

        verify(cascadeDeleter).deleteOpinions(List.of(1L));
        verify(passage, never()).delete();
        verify(passageRepository, never()).findById(eq(100L));
    }
}
