package com.nexters.palang.domain.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private PassageRepository passageRepository;
    @Mock
    private OpinionRepository opinionRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private DecorationRepository decorationRepository;
    @Mock
    private OpinionLikeRepository opinionLikeRepository;
    @Mock
    private DeviceTokenRepository deviceTokenRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserBlockRepository userBlockRepository;
    @Mock
    private UserBookStatusRepository userBookStatusRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AdminUserService adminUserService;
    private User target;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(
                userRepository, groupRepository, groupMemberRepository, passageRepository, opinionRepository,
                commentRepository, decorationRepository, opinionLikeRepository, deviceTokenRepository,
                notificationRepository, reportRepository, userBlockRepository, userBookStatusRepository,
                refreshTokenRepository);
        target = user(10L);
    }

    private User user(Long id) {
        User built = User.builder().nickname("테스트유저" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    @Test
    @DisplayName("존재하지 않는 유저를 삭제하려 하면 예외가 발생한다")
    void deleteFailsWhenUserNotFound() {
        given(userRepository.findById(target.getId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.deleteUser(target.getId())).isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("다른 멤버가 있는 모임의 호스트면 삭제가 차단된다")
    void deleteFailsWhenHostingGroupWithOtherMembers() {
        given(userRepository.findById(target.getId())).willReturn(Optional.of(target));
        Group hostedGroup = mock(Group.class);
        given(hostedGroup.getId()).willReturn(100L);
        given(hostedGroup.getName()).willReturn("독서모임A");
        given(groupRepository.findAllByHostId(target.getId())).willReturn(List.of(hostedGroup));
        given(groupMemberRepository.countByGroupId(100L)).willReturn(2L);

        assertThatThrownBy(() -> adminUserService.deleteUser(target.getId()))
                .isInstanceOf(AdminException.class)
                .hasFieldOrPropertyWithValue("errorCode", AdminErrorCode.USER_DELETE_BLOCKED_BY_HOSTED_GROUP);

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("다른 유저의 의견이 달린 흔적을 작성했으면 삭제가 차단된다")
    void deleteFailsWhenPassageHasOthersOpinions() {
        given(userRepository.findById(target.getId())).willReturn(Optional.of(target));
        given(groupRepository.findAllByHostId(target.getId())).willReturn(List.of());

        Passage ownPassage = mock(Passage.class);
        given(ownPassage.getId()).willReturn(200L);
        given(passageRepository.findAllByCreatorId(target.getId())).willReturn(List.of(ownPassage));
        given(opinionRepository.findPassageIdsWithOpinionsFromOtherUsers(List.of(200L), target.getId()))
                .willReturn(List.of(200L));

        assertThatThrownBy(() -> adminUserService.deleteUser(target.getId()))
                .isInstanceOf(AdminException.class)
                .hasFieldOrPropertyWithValue("errorCode", AdminErrorCode.USER_DELETE_BLOCKED_BY_PASSAGE);

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("차단 조건이 없으면 유저와 연관 데이터가 삭제된다")
    void deleteRemovesUserAndRelatedData() {
        given(userRepository.findById(target.getId())).willReturn(Optional.of(target));
        given(groupRepository.findAllByHostId(target.getId())).willReturn(List.of());
        given(passageRepository.findAllByCreatorId(target.getId())).willReturn(List.of());
        given(opinionRepository.findIdsByUserId(target.getId())).willReturn(List.of());
        given(commentRepository.findIdsByUserId(target.getId())).willReturn(List.of());

        adminUserService.deleteUser(target.getId());

        verify(groupMemberRepository).deleteAllByUserId(target.getId());
        verify(deviceTokenRepository).deleteAllByUserId(target.getId());
        verify(notificationRepository).deleteAllByReceiverId(target.getId());
        verify(reportRepository).deleteAllByReporterId(target.getId());
        verify(userBlockRepository).deleteAllByBlockerIdOrBlockedId(target.getId(), target.getId());
        verify(userBookStatusRepository).deleteAllByUserId(target.getId());
        verify(refreshTokenRepository).deleteAllByUserId(target.getId());
        verify(userRepository).delete(target);
    }
}
