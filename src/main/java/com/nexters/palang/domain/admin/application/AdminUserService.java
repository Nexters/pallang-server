package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.admin.common.error.AdminErrorCode;
import com.nexters.palang.domain.admin.common.error.AdminException;
import com.nexters.palang.domain.auth.infrastructure.RefreshTokenRepository;
import com.nexters.palang.domain.block.infrastructure.UserBlockRepository;
import com.nexters.palang.domain.book.infrastructure.UserBookStatusRepository;
import com.nexters.palang.domain.comment.infrastructure.CommentRepository;
import com.nexters.palang.domain.decoration.infrastructure.DecorationRepository;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.notification.infrastructure.DeviceTokenRepository;
import com.nexters.palang.domain.notification.infrastructure.NotificationRepository;
import com.nexters.palang.domain.opinion.infrastructure.OpinionLikeRepository;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import com.nexters.palang.domain.report.infrastructure.ReportRepository;
import com.nexters.palang.domain.user.common.error.UserErrorCode;
import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// prod에 쌓인 테스트/QA 계정을 관리자 페이지에서 정리하기 위한 하드 삭제(issue #155). User.withdraw()
// (회원탈퇴, 익명화만 하고 실제 행은 남기는 정책)와는 완전히 별개의 관리자 전용 경로이며, 일반 유저 플로우와
// 섞이지 않는다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final PassageRepository passageRepository;
    private final OpinionRepository opinionRepository;
    private final CommentRepository commentRepository;
    private final DecorationRepository decorationRepository;
    private final OpinionLikeRepository opinionLikeRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRepository notificationRepository;
    private final ReportRepository reportRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserBookStatusRepository userBookStatusRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public Page<AdminUserSearchResult> searchUsers(String keyword, Pageable pageable) {
        Page<User> users = userRepository.findByNicknameContainingOrEmailContaining(keyword, keyword, pageable);
        return users.map(user -> new AdminUserSearchResult(user, groupRepository.countByHostId(user.getId())));
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        List<Long> soloHostedGroupIds = validateDeletableAndCollectSoloHostedGroups(userId);

        Set<Long> passageIdsToDelete = collectPassageIdsToDelete(userId, soloHostedGroupIds);
        Set<Long> opinionIdsToDelete = collectOpinionIdsToDelete(userId, passageIdsToDelete);

        deleteOpinionSubContent(userId, opinionIdsToDelete);
        if (!opinionIdsToDelete.isEmpty()) {
            opinionRepository.deleteAllById(opinionIdsToDelete);
        }
        if (!passageIdsToDelete.isEmpty()) {
            passageRepository.deleteAllById(passageIdsToDelete);
        }

        groupMemberRepository.deleteAllByUserId(userId);
        if (!soloHostedGroupIds.isEmpty()) {
            groupRepository.deleteAllById(soloHostedGroupIds);
        }

        deviceTokenRepository.deleteAllByUserId(userId);
        notificationRepository.deleteAllByReceiverId(userId);
        reportRepository.deleteAllByReporterId(userId);
        userBlockRepository.deleteAllByBlockerIdOrBlockedId(userId, userId);
        userBookStatusRepository.deleteAllByUserId(userId);
        refreshTokenRepository.deleteAllByUserId(userId);

        userRepository.delete(user);
    }

    // 다른 멤버가 있는 모임의 호스트면 삭제를 막는다(그 모임의 다른 멤버 데이터까지 함께 사라지므로).
    // 통과하면, 혼자뿐인(=삭제해도 collateral이 없는) 호스트 모임 id 목록을 돌려준다.
    private List<Long> validateDeletableAndCollectSoloHostedGroups(Long userId) {
        List<Group> hostedGroups = groupRepository.findAllByHostId(userId);
        List<Long> soloHostedGroupIds = new ArrayList<>();
        List<String> blockingGroupNames = new ArrayList<>();
        for (Group group : hostedGroups) {
            if (groupMemberRepository.countByGroupId(group.getId()) > 1) {
                blockingGroupNames.add(group.getName());
            } else {
                soloHostedGroupIds.add(group.getId());
            }
        }
        if (!blockingGroupNames.isEmpty()) {
            throw new AdminException(AdminErrorCode.USER_DELETE_BLOCKED_BY_HOSTED_GROUP,
                    "다른 멤버가 있는 모임의 호스트여서 삭제할 수 없습니다: " + String.join(", ", blockingGroupNames));
        }

        List<Long> ownPassageIds = passageRepository.findAllByCreatorId(userId).stream().map(Passage::getId).toList();
        if (!ownPassageIds.isEmpty()) {
            List<Long> blockingPassageIds =
                    opinionRepository.findPassageIdsWithOpinionsFromOtherUsers(ownPassageIds, userId);
            if (!blockingPassageIds.isEmpty()) {
                throw new AdminException(AdminErrorCode.USER_DELETE_BLOCKED_BY_PASSAGE,
                        "다른 사용자의 의견이 달린 흔적을 작성해서 삭제할 수 없습니다 (흔적 id: "
                                + blockingPassageIds.stream().map(String::valueOf).collect(Collectors.joining(", "))
                                + ")");
            }
        }
        return soloHostedGroupIds;
    }

    // 삭제 대상 대목 = 이 유저가 작성한 대목 전부 ∪ 이 유저 혼자만 있던 모임 소속 대목 전부.
    private Set<Long> collectPassageIdsToDelete(Long userId, List<Long> soloHostedGroupIds) {
        Set<Long> passageIds = new LinkedHashSet<>();
        passageRepository.findAllByCreatorId(userId).forEach(p -> passageIds.add(p.getId()));
        if (!soloHostedGroupIds.isEmpty()) {
            passageRepository.findAllByGroupIdIn(soloHostedGroupIds).forEach(p -> passageIds.add(p.getId()));
        }
        return passageIds;
    }

    // 삭제 대상 의견 = 삭제될 대목에 달린 의견 전부(작성자 무관) ∪ 이 유저 본인이 남긴 의견 전부(대목 무관).
    private Set<Long> collectOpinionIdsToDelete(Long userId, Set<Long> passageIdsToDelete) {
        Set<Long> opinionIds = new LinkedHashSet<>();
        if (!passageIdsToDelete.isEmpty()) {
            opinionIds.addAll(opinionRepository.findIdsByPassageIdIn(List.copyOf(passageIdsToDelete)));
        }
        opinionIds.addAll(opinionRepository.findIdsByUserId(userId));
        return opinionIds;
    }

    // 댓글은 자기참조 FK(parent_comment_id)가 있어 답글부터 지워야 한다. 이 유저의 (삭제 대상에 포함되지
    // 않는) 댓글에 달린 답글을 먼저 지운 뒤, 삭제될 의견에 달린 댓글/데코/좋아요를 한 번에 지우고, 마지막으로
    // 이 유저의 나머지 댓글/좋아요를 지운다. 다른 유저가 이 유저의 의견/댓글에 남긴 반응(댓글/좋아요/데코)도
    // 함께 사라지는데, 이는 "본인 콘텐츠에 달린 반응"이라 모임 호스트/대목 소유 케이스와 달리 별도 차단
      // 조건을 두지 않기로 했다(issue #155 참고 사항).
    private void deleteOpinionSubContent(Long userId, Set<Long> opinionIdsToDelete) {
        List<Long> ownCommentIds = commentRepository.findIdsByUserId(userId);
        if (!ownCommentIds.isEmpty()) {
            commentRepository.deleteAllByParentCommentIdIn(ownCommentIds);
        }
        if (!opinionIdsToDelete.isEmpty()) {
            List<Long> opinionIdList = List.copyOf(opinionIdsToDelete);
            commentRepository.deleteAllByOpinionIdIn(opinionIdList);
            decorationRepository.deleteAllByOpinionIdIn(opinionIdList);
            opinionLikeRepository.deleteAllByOpinionIdIn(opinionIdList);
        }
        commentRepository.deleteAllByUserId(userId);
        opinionLikeRepository.deleteAllByUserId(userId);
    }
}
