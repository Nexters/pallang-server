package com.nexters.palang.domain.comment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.domain.comment.common.NestedReplyNotAllowedException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommentTest {

    @Test
    @DisplayName("원댓글에 답글을 달면 부모가 원댓글인 대댓글이 생성된다")
    void createReplyToRootComment() {
        Opinion opinion = Opinion.builder().build();
        User user = User.builder().build();
        Comment root = Comment.root(opinion, user, "원댓글");

        Comment reply = Comment.reply(root, user, "대댓글");

        assertThat(reply.getParentComment()).isEqualTo(root);
    }

    @Test
    @DisplayName("대댓글에 다시 답글을 달면 예외가 발생한다")
    void replyToReplyThrowsException() {
        Opinion opinion = Opinion.builder().build();
        User user = User.builder().build();
        Comment root = Comment.root(opinion, user, "원댓글");
        Comment reply = Comment.reply(root, user, "대댓글");

        assertThatThrownBy(() -> Comment.reply(reply, user, "대댓글의 답글"))
                .isInstanceOf(NestedReplyNotAllowedException.class);
    }
}
