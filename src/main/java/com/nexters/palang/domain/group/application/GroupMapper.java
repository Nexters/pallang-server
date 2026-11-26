package com.nexters.palang.domain.group.application;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupReadingSummary;
import com.nexters.palang.domain.group.presentation.dto.GroupDetailResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupInvitationPreviewResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupInviteLinkResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupListResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupMemberListResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupMemberPreviewResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupMemberResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupSummaryResponse;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.common.response.PageInfo;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;

public final class GroupMapper {

    private GroupMapper() {
    }

    public static GroupDetailResponse toDetailResponse(GroupDetail detail, Long currentUserId) {
        Group group = detail.group();
        GroupBookEntry primaryEntry = detail.primaryEntry();
        Book book = primaryEntry.getBook();
        User host = group.getHost();
        return new GroupDetailResponse(
                group.getId(), group.getName(), book.getId(), book.getTitle(), book.getAuthor(), book.getCoverImageUrl(),
                host.getId(), host.getNickname(), group.getCapacity(), detail.memberCount(),
                primaryEntry.getStartDate(), primaryEntry.getEndDate(), detail.ended(), host.getId().equals(currentUserId),
                Objects.requireNonNullElse(group.getVersion(), 0L));
    }

    public static GroupSummaryResponse toSummaryResponse(GroupSummaryProjection projection) {
        GroupReadingSummary readingSummary = projection.readingSummary();
        List<GroupMemberPreviewResponse> memberPreviews = projection.memberPreviews().stream()
                .map(preview -> new GroupMemberPreviewResponse(
                        preview.userId(), preview.nickname(), preview.profileImageUrl()))
                .toList();
        long extraMemberCount = Math.max(projection.memberCount() - memberPreviews.size(), 0L);
        return new GroupSummaryResponse(
                projection.groupId(), projection.name(), projection.bookId(), projection.bookTitle(),
                projection.bookCoverImageUrl(), projection.memberCount(), projection.capacity(),
                projection.startDate(), projection.endDate(), projection.ended(), projection.isHost(),
                readingSummary.readingBookCount(), readingSummary.latestReadingEndDate(),
                readingSummary.scheduleStatus(), memberPreviews, extraMemberCount);
    }

    public static GroupListResponse toListResponse(Page<GroupSummaryProjection> page) {
        return new GroupListResponse(page.map(GroupMapper::toSummaryResponse).getContent(), PageInfo.from(page));
    }

    // 모임장/본인 여부는 계정 ID로 판별한다. 본인이 모임장이면 같은 행에 두 표시가 모두 켜진다.
    public static GroupMemberResponse toMemberResponse(GroupMember member, Long hostUserId, Long viewerId) {
        User user = member.getUser();
        return new GroupMemberResponse(
                user.getId(), user.getNickname(), user.getProfileImageUrl(), member.getRole(), member.getJoinedAt(),
                user.getId().equals(hostUserId), user.getId().equals(viewerId));
    }

    // host는 LAZY 프록시지만 id만 읽으므로 초기화 없이 안전하다.
    public static GroupMemberListResponse toMemberListResponse(GroupMemberPage page) {
        Group group = page.group();
        Long hostUserId = group.getHost().getId();
        Page<GroupMember> members = page.members();
        return new GroupMemberListResponse(
                members.map(member -> toMemberResponse(member, hostUserId, page.viewerId())).getContent(),
                PageInfo.from(members), members.getTotalElements(), group.getCapacity());
    }

    public static GroupInviteLinkResponse toInviteLinkResponse(GroupInviteLink inviteLink) {
        Group group = inviteLink.group();
        return new GroupInviteLinkResponse(group.getId(), inviteLink.inviteCode(), inviteLink.memberCount(),
                group.getCapacity(), inviteLink.isFull());
    }

    public static GroupInvitationPreviewResponse toInvitationPreviewResponse(GroupInvitationPreview preview) {
        Group group = preview.group();
        Book book = preview.primaryEntry().getBook();
        boolean full = preview.memberCount() >= group.getCapacity();
        return new GroupInvitationPreviewResponse(
                group.getId(), group.getName(), book.getTitle(), book.getAuthor(), book.getCoverImageUrl(),
                group.getCapacity(), preview.memberCount(), full, preview.alreadyJoined());
    }
}
