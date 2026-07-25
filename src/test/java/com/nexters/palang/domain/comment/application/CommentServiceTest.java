package com.nexters.palang.domain.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.nexters.palang.domain.comment.common.CommentException;
import com.nexters.palang.domain.comment.common.NestedReplyNotAllowedException;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.comment.infrastructure.CommentQueryRepository;
import com.nexters.palang.domain.comment.infrastructure.CommentRepository;
import com.nexters.palang.domain.comment.presentation.dto.CreateCommentRequest;
import com.nexters.palang.domain.comment.presentation.dto.UpdateCommentRequest;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentQueryRepository commentQueryRepository;

    @Mock
    private OpinionRepository opinionRepository;

    @Mock
    private UserRepository userRepository;

    private CommentService commentService;

    private Opinion opinion;
    private User writer;
    private User otherUser;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, commentQueryRepository, opinionRepository, userRepository);
        opinion = opinion(1L);
        writer = user(10L);
        otherUser = user(20L);
    }

    private Opinion opinion(Long id) {
        Opinion built = Opinion.builder().user(writer).content("흔적 내용").build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private User user(Long id) {
        User built = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private Comment rootComment(Long id, Opinion targetOpinion, User author) {
        Comment comment = Comment.root(targetOpinion, author, "원댓글");
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    private Comment replyComment(Long id, Comment parent, User author) {
        Comment comment = Comment.reply(parent, author, "답글");
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    @Test
    @DisplayName("존재하지 않는 흔적의 댓글 목록을 조회하면 예외가 발생한다")
    void getRootCommentsFailsWhenOpinionNotFound() {
        given(opinionRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getRootComments(999L, PageRequest.of(0, 20)))
                .isInstanceOf(OpinionException.class);
    }

    @Test
    @DisplayName("삭제된 흔적의 댓글 목록을 조회하면 예외가 발생한다")
    void getRootCommentsFailsWhenOpinionDeleted() {
        opinion.delete();
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));

        assertThatThrownBy(() -> commentService.getRootComments(1L, PageRequest.of(0, 20)))
                .isInstanceOf(OpinionException.class);
    }

    @Test
    @DisplayName("원댓글 목록을 조회하면 답글은 5개까지 미리보기로, 6개 이상이면 hasMoreReplies가 true로 반환된다")
    void getRootCommentsGroupsRepliesWithPreviewAndCount() {
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        Comment root1 = rootComment(1L, opinion, writer);
        Comment root2 = rootComment(2L, opinion, writer);
        Pageable pageable = PageRequest.of(0, 20);
        given(commentQueryRepository.findRootComments(1L, pageable))
                .willReturn(new PageImpl<>(List.of(root1, root2), pageable, 2));

        List<Comment> root1Replies = List.of(
                replyComment(11L, root1, writer), replyComment(12L, root1, writer),
                replyComment(13L, root1, writer), replyComment(14L, root1, writer),
                replyComment(15L, root1, writer), replyComment(16L, root1, writer));
        given(commentQueryRepository.findRepliesByParentIds(List.of(1L, 2L))).willReturn(root1Replies);

        Page<RootCommentGroup> results = commentService.getRootComments(1L, pageable);

        RootCommentGroup group1 = results.getContent().get(0);
        assertThat(group1.replyPreview()).hasSize(5);
        assertThat(group1.replyCount()).isEqualTo(6);
        assertThat(group1.hasMoreReplies()).isTrue();

        RootCommentGroup group2 = results.getContent().get(1);
        assertThat(group2.replyPreview()).isEmpty();
        assertThat(group2.replyCount()).isZero();
        assertThat(group2.hasMoreReplies()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 원댓글의 답글을 조회하면 예외가 발생한다")
    void getRepliesFailsWhenParentCommentNotFound() {
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getReplies(999L, PageRequest.of(0, 20)))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("답글 더보기를 요청하면 해당 원댓글의 답글을 페이지네이션으로 반환한다")
    void getRepliesReturnsPageFromQueryRepository() {
        Comment root = rootComment(1L, opinion, writer);
        given(commentRepository.findById(1L)).willReturn(Optional.of(root));
        Pageable pageable = PageRequest.of(0, 5);
        Page<Comment> expected = new PageImpl<>(List.of(replyComment(11L, root, writer)), pageable, 1);
        given(commentQueryRepository.findReplies(1L, pageable)).willReturn(expected);

        Page<Comment> results = commentService.getReplies(1L, pageable);

        assertThat(results).isEqualTo(expected);
    }

    @Test
    @DisplayName("원댓글을 작성하면 부모 댓글 없이 저장된다")
    void createRootComment() {
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(userRepository.getReferenceById(10L)).willReturn(writer);
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> invocation.getArgument(0));

        Comment created = commentService.createComment(1L, 10L, new CreateCommentRequest(null, "내용"));

        assertThat(created.getParentComment()).isNull();
        assertThat(created.getContent()).isEqualTo("내용");
        assertThat(created.getUser()).isEqualTo(writer);
    }

    @Test
    @DisplayName("존재하지 않는 흔적에 댓글을 작성하면 예외가 발생한다")
    void createCommentFailsWhenOpinionNotFound() {
        given(opinionRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(999L, 10L, new CreateCommentRequest(null, "내용")))
                .isInstanceOf(OpinionException.class);
    }

    @Test
    @DisplayName("원댓글에 답글을 작성하면 부모 댓글과 함께 저장된다")
    void createReplyComment() {
        Comment root = rootComment(1L, opinion, writer);
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(commentRepository.findById(1L)).willReturn(Optional.of(root));
        given(userRepository.getReferenceById(20L)).willReturn(otherUser);
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> invocation.getArgument(0));

        Comment created = commentService.createComment(1L, 20L, new CreateCommentRequest(1L, "답글 내용"));

        assertThat(created.getParentComment()).isEqualTo(root);
        assertThat(created.getContent()).isEqualTo("답글 내용");
    }

    @Test
    @DisplayName("존재하지 않는 부모 댓글에 답글을 작성하면 예외가 발생한다")
    void createReplyFailsWhenParentNotFound() {
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(1L, 10L, new CreateCommentRequest(999L, "내용")))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("다른 흔적에 속한 댓글을 부모로 답글을 작성하면 예외가 발생한다")
    void createReplyFailsWhenParentBelongsToDifferentOpinion() {
        Opinion otherOpinion = opinion(2L);
        Comment parentOfOtherOpinion = rootComment(1L, otherOpinion, writer);
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(commentRepository.findById(1L)).willReturn(Optional.of(parentOfOtherOpinion));

        assertThatThrownBy(() -> commentService.createComment(1L, 10L, new CreateCommentRequest(1L, "내용")))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("답글에 다시 답글을 작성하면 1-depth 제약 예외가 그대로 전파된다")
    void createReplyToReplyPropagatesNestedReplyException() {
        Comment root = rootComment(1L, opinion, writer);
        Comment reply = replyComment(2L, root, writer);
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(commentRepository.findById(2L)).willReturn(Optional.of(reply));

        assertThatThrownBy(() -> commentService.createComment(1L, 10L, new CreateCommentRequest(2L, "내용")))
                .isInstanceOf(NestedReplyNotAllowedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 수정하면 예외가 발생한다")
    void modifyCommentFailsWhenNotFound() {
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.modifyComment(999L, 10L, new UpdateCommentRequest("수정")))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("이미 삭제된 댓글을 수정하면 예외가 발생한다")
    void modifyCommentFailsWhenAlreadyDeleted() {
        Comment comment = rootComment(1L, opinion, writer);
        comment.delete();
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.modifyComment(1L, 10L, new UpdateCommentRequest("수정")))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("본인이 작성하지 않은 댓글을 수정하면 예외가 발생한다")
    void modifyCommentFailsWhenNotOwner() {
        Comment comment = rootComment(1L, opinion, writer);
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.modifyComment(1L, 20L, new UpdateCommentRequest("수정")))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("본인이 작성한 댓글을 수정하면 내용이 변경된다")
    void modifyCommentUpdatesContent() {
        Comment comment = rootComment(1L, opinion, writer);
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        Comment modified = commentService.modifyComment(1L, 10L, new UpdateCommentRequest("수정된 내용"));

        assertThat(modified.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("본인이 작성하지 않은 댓글을 삭제하면 예외가 발생한다")
    void removeCommentFailsWhenNotOwner() {
        Comment comment = rootComment(1L, opinion, writer);
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.removeComment(1L, 20L)).isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("본인이 작성한 댓글을 삭제하면 소프트 삭제된다")
    void removeCommentSoftDeletesComment() {
        Comment comment = rootComment(1L, opinion, writer);
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        commentService.removeComment(1L, 10L);

        assertThat(comment.isDeleted()).isTrue();
    }
}
