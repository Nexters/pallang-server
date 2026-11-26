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

    public static GroupMemberResponse toMemberResponse(GroupMember member) {
        User user = member.getUser();
        return new GroupMemberResponse(
                user.getId(), user.getNickname(), user.getProfileImageUrl(), member.getRole(), member.getJoinedAt());
    }

    public static GroupMemberListResponse toMemberListResponse(Page<GroupMember> page) {
        return new GroupMemberListResponse(page.map(GroupMapper::toMemberResponse).getContent(), PageInfo.from(page));
    }

    public static GroupInviteLinkResponse toInviteLinkResponse(Long groupId, String inviteCode) {
        return new GroupInviteLinkResponse(groupId, inviteCode);
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
