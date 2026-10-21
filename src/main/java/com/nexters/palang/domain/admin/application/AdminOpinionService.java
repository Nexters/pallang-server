package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.admin.common.error.AdminErrorCode;
import com.nexters.palang.domain.admin.common.error.AdminException;
import com.nexters.palang.domain.notification.application.NotificationCreationService;
import com.nexters.palang.domain.notification.domain.RemovalReason;
import com.nexters.palang.domain.opinion.common.error.OpinionErrorCode;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import com.nexters.palang.domain.report.domain.ReportTargetType;
import com.nexters.palang.domain.report.infrastructure.ReportCountProjection;
import com.nexters.palang.domain.report.infrastructure.ReportRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 페이지에서 의견(Opinion)을 조회/수정/삭제하기 위한 서비스(issue #155 후속). OpinionService의
// modifyOpinion/removeOpinion은 작성자 본인만 호출할 수 있어(validateOwner) 관리자 용도로 재사용할 수
// 없다 — 소유권 검사를 우회하는 별도 경로가 필요하다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOpinionService {

    private final OpinionRepository opinionRepository;
    private final PassageRepository passageRepository;
    private final AdminCascadeDeleter cascadeDeleter;
    private final ReportRepository reportRepository;
    private final NotificationCreationService notificationCreationService;

    public Page<Opinion> searchOpinions(String keyword, Pageable pageable) {
        return opinionRepository.findByContentContaining(keyword, pageable);
    }

    // 관리자 페이지에서 대목 행을 펼쳐 그 대목에 달린 의견을 한 세트로 보여주기 위한 조회(이슈 #204).
    public Page<Opinion> searchOpinionsByPassage(Long passageId, Pageable pageable) {
        return opinionRepository.findByPassageIdOrderByCreatedAtAscIdAsc(passageId, pageable);
    }

    // 목록의 의견마다 신고 수 뱃지를 붙이기 위한 집계. 신고가 없는 의견은 결과에 없다(호출부에서 0으로 취급).
    public Map<Long, Long> countReports(Collection<Long> opinionIds) {
        if (opinionIds.isEmpty()) {
            return Map.of();
        }
        return reportRepository.countByTargets(ReportTargetType.OPINION, opinionIds).stream()
                .collect(Collectors.toMap(ReportCountProjection::getTargetId, ReportCountProjection::getReportCount));
    }

    public long countReports(Long opinionId) {
        return reportRepository.countByTargetTypeAndTargetId(ReportTargetType.OPINION, opinionId);
    }

    @Transactional
    public Opinion updateOpinion(Long opinionId, String content) {
        Opinion opinion = getExistingOpinion(opinionId);
        opinion.updateContent(content);
        return opinion;
    }

    // 의견과 거기 달린 댓글/데코/좋아요를 하드 삭제한다. 그 대목에 남은 살아있는 의견이 더 없으면
    // (OpinionService.removeOpinion과 동일한 규칙) 대목도 함께 소프트 삭제한다. 되돌릴 수 없다.
    // 작성자에게 알림을 보내지 않는다 — 테스트/QA 데이터 정리용이다. 실제 유저 게시물은 removeOpinionWithReason을 쓴다.
    @Transactional
    public void deleteOpinion(Long opinionId) {
        Opinion opinion = getExistingOpinion(opinionId);
        Long passageId = opinion.getPassage().getId();
        boolean wasLastLiveOpinion = !opinionRepository.existsByPassageIdAndDeletedAtIsNullAndIdNot(passageId, opinionId);

        cascadeDeleter.deleteOpinions(List.of(opinionId));

        if (wasLastLiveOpinion) {
            passageRepository.findById(passageId).ifPresent(passage -> passage.delete());
        }
    }

    // 운영 정책에 따른 사유 선택 삭제(이슈 #208). 삭제 범위는 deleteOpinion과 같고(의견 + 다른 의견이 없으면 대목),
    // 삭제 전에 작성자가 알아볼 수 있는 식별 정보를 떠서 운영 안내 알림으로 보낸다. 모임 게시물은 모임 안에서만
    // 보이는 글이라 신고가 접수된 경우에만 관리자가 개입할 수 있다.
    @Transactional
    public void removeOpinionWithReason(Long opinionId, RemovalReason reason) {
        Opinion opinion = getExistingOpinion(opinionId);
        Passage passage = opinion.getPassage();
        if (passage.getGroup() != null && countReports(opinionId) == 0) {
            throw new AdminException(AdminErrorCode.GROUP_CONTENT_REMOVAL_REQUIRES_REPORT);
        }

        Long authorId = opinion.getUser().getId();
        String bookTitle = passage.getBook().getTitle();
        LocalDateTime writtenAt = opinion.getCreatedAt();
        // "발췌란에 의견 작성" 사유는 문제가 된 글이 발췌문 쪽이라 그 내용을 보여준다.
        String content = reason == RemovalReason.PASSAGE_FIELD_MISUSE ? passage.getQuotedText() : opinion.getContent();

        deleteOpinion(opinionId);
        notificationCreationService.createContentRemovedNotice(authorId, reason, bookTitle, writtenAt, content);
    }

    private Opinion getExistingOpinion(Long opinionId) {
        return opinionRepository.findWithAssociationsById(opinionId)
                .orElseThrow(() -> new OpinionException(OpinionErrorCode.OPINION_NOT_FOUND));
    }
}
