package com.nexters.palang.domain.notification.domain;

import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 유저당 row가 없으면 "전체 허용"(7개 토글 모두 true)으로 취급한다. PUT 호출 전까지는 row를 만들지 않는다.
// 마이페이지 > 알림 설정 화면(Figma 기준) 구성: 푸시 마스터 1개 + 활동 2개 + 모임 4개.
@Getter
@Entity
@Table(
        name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(name = "uq_notification_preferences_user", columnNames = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 마스터 토글. 꺼져 있으면 아래 카테고리 설정과 무관하게 전부 차단한다.
    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    // 활동(전역) - 의견 및 댓글: OPINION_COMMENTED, groupId가 없는 경우.
    @Column(name = "activity_comment_enabled", nullable = false)
    private boolean activityCommentEnabled;

    // 활동(전역) - 좋아요: OPINION_LIKED, groupId가 없는 경우.
    @Column(name = "activity_like_enabled", nullable = false)
    private boolean activityLikeEnabled;

    // 모임 - 초대 및 가입: GROUP_JOINED, GROUP_MEMBER_JOINED.
    @Column(name = "group_invite_join_enabled", nullable = false)
    private boolean groupInviteJoinEnabled;

    // 모임 - 의견 및 댓글: OPINION_COMMENTED, groupId가 있는 경우.
    @Column(name = "group_comment_enabled", nullable = false)
    private boolean groupCommentEnabled;

    // 모임 - 좋아요: OPINION_LIKED, groupId가 있는 경우.
    @Column(name = "group_like_enabled", nullable = false)
    private boolean groupLikeEnabled;

    // 모임 - 일정 변경: GROUP_SCHEDULE_CHANGED, GROUP_ENDING_SOON. Figma 문구가 두 상황을 한 토글로 묶는다.
    @Column(name = "group_schedule_enabled", nullable = false)
    private boolean groupScheduleEnabled;

    @Builder
    private NotificationPreference(
            User user,
            boolean pushEnabled,
            boolean activityCommentEnabled,
            boolean activityLikeEnabled,
            boolean groupInviteJoinEnabled,
            boolean groupCommentEnabled,
            boolean groupLikeEnabled,
            boolean groupScheduleEnabled
    ) {
        this.user = user;
        this.pushEnabled = pushEnabled;
        this.activityCommentEnabled = activityCommentEnabled;
        this.activityLikeEnabled = activityLikeEnabled;
        this.groupInviteJoinEnabled = groupInviteJoinEnabled;
        this.groupCommentEnabled = groupCommentEnabled;
        this.groupLikeEnabled = groupLikeEnabled;
        this.groupScheduleEnabled = groupScheduleEnabled;
    }

    public void update(
            boolean pushEnabled,
            boolean activityCommentEnabled,
            boolean activityLikeEnabled,
            boolean groupInviteJoinEnabled,
            boolean groupCommentEnabled,
            boolean groupLikeEnabled,
            boolean groupScheduleEnabled
    ) {
        this.pushEnabled = pushEnabled;
        this.activityCommentEnabled = activityCommentEnabled;
        this.activityLikeEnabled = activityLikeEnabled;
        this.groupInviteJoinEnabled = groupInviteJoinEnabled;
        this.groupCommentEnabled = groupCommentEnabled;
        this.groupLikeEnabled = groupLikeEnabled;
        this.groupScheduleEnabled = groupScheduleEnabled;
    }
}
