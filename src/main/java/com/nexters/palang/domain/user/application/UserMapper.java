package com.nexters.palang.domain.user.application;

import com.nexters.palang.domain.opinion.application.LikedOpinionProjection;
import com.nexters.palang.domain.opinion.application.MyOpinionProjection;
import com.nexters.palang.domain.passage.application.MyPassageProjection;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.presentation.dto.LikedOpinionListResponse;
import com.nexters.palang.domain.user.presentation.dto.LikedOpinionResponse;
import com.nexters.palang.domain.user.presentation.dto.MeResponse;
import com.nexters.palang.domain.user.presentation.dto.MyOpinionListResponse;
import com.nexters.palang.domain.user.presentation.dto.MyOpinionResponse;
import com.nexters.palang.domain.user.presentation.dto.MyPassageListResponse;
import com.nexters.palang.domain.user.presentation.dto.MyPassageResponse;
import com.nexters.palang.global.common.response.PageInfo;
import org.springframework.data.domain.Page;

public final class UserMapper {

    private UserMapper() {
    }

    public static MeResponse toMeResponse(User user, long opinionCount) {
        return new MeResponse(
                user.getId(), user.getNickname(), user.getEmail(), user.getProfileImageUrl(),
                user.getBackgroundColor(), user.getSnsProvider(), opinionCount);
    }

    public static MyOpinionResponse toMyOpinionResponse(MyOpinionProjection projection) {
        return new MyOpinionResponse(
                projection.opinionId(), projection.bookId(), projection.bookTitle(), projection.author(),
                projection.bookCoverImageUrl(), projection.passageId(), projection.quotedText(), projection.pageNumber(),
                projection.content(), projection.likeCount(), projection.createdAt());
    }

    public static MyOpinionListResponse toMyOpinionListResponse(Page<MyOpinionProjection> projections) {
        return new MyOpinionListResponse(
                projections.map(UserMapper::toMyOpinionResponse).getContent(), PageInfo.from(projections));
    }

    public static LikedOpinionResponse toLikedOpinionResponse(LikedOpinionProjection projection) {
        return new LikedOpinionResponse(
                projection.opinionId(), projection.bookId(), projection.bookTitle(), projection.bookCoverImageUrl(),
                projection.passageId(), projection.quotedText(), projection.pageNumber(),
                projection.content(), projection.likeCount(), projection.createdAt(), projection.likedAt());
    }

    public static LikedOpinionListResponse toLikedOpinionListResponse(Page<LikedOpinionProjection> projections) {
        return new LikedOpinionListResponse(
                projections.map(UserMapper::toLikedOpinionResponse).getContent(), PageInfo.from(projections));
    }

    public static MyPassageResponse toMyPassageResponse(MyPassageProjection projection) {
        return new MyPassageResponse(
                projection.passageId(), projection.bookId(), projection.pageNumber(), projection.quotedText(),
                projection.isSpoiler(), projection.createdAt());
    }

    public static MyPassageListResponse toMyPassageListResponse(Page<MyPassageProjection> projections) {
        return new MyPassageListResponse(
                projections.map(UserMapper::toMyPassageResponse).getContent(), PageInfo.from(projections));
    }
}
