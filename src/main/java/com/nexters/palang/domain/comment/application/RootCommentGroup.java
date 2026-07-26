package com.nexters.palang.domain.comment.application;

import com.nexters.palang.domain.comment.domain.Comment;
import java.util.List;

public record RootCommentGroup(Comment comment, List<Comment> replyPreview, long replyCount) {

    public boolean hasMoreReplies() {
        return replyCount > replyPreview.size();
    }
}
