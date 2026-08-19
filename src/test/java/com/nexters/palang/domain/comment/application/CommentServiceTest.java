package com.nexters.palang.domain.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.comment.common.CommentException;
import com.nexters.palang.domain.comment.common.NestedReplyNotAllowedException;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.comment.domain.event.CommentCreatedEvent;
import com.nexters.palang.domain.comment.infrastructure.CommentQueryRepository;
import com.nexters.palang.domain.comment.infrastructure.CommentRepository;
import com.nexters.palang.domain.comment.presentation.dto.CreateCommentRequest;
import com.nexters.palang.domain.comment.presentation.dto.UpdateCommentRequest;
import com.nexters.palang.domain.group.application.GroupAccessValidator;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GroupAccessValidator groupAccessValidator;

    private CommentService commentService;

    private Opinion opinion;
    private User writer;
    private User otherUser;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(
                commentRepository, commentQueryRepository, opinionRepository, userRepository,
                eventPublisher, groupAccessValidator);
        writer = user(10L);
        opinion = opinion(1L);
        otherUser = user(20L);
    }

    private Opinion opinion(Long id) {
        return opinion(id, null);
    }

    private Opinion opinion(Long id, Group group) {
        Passage passage = Passage.builder().group(group).build();
        Opinion built = Opinion.builder().user(writer).passage(passage).content("흔적 내용").build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private Group group(Long id) {
        Book book = Book.builder().title("제목").author("작가").publisher("출판사").pageCount(300).build();
        Group built = Group.create("모임", book, writer, 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));
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

        assertThatThrownBy(() -> commentService.getRootComments(999L, PageRequest.of(0, 20), null))
                .isInstanceOf(OpinionException.class);
    }

    @Test
    @DisplayName("삭제된 흔적의 댓글 목록을 조회하면 예외가 발생한다")
    void getRootCommentsFailsWhenOpinionDeleted() {
        opinion.delete();
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));

        assertThatThrownBy(() -> commentService.getRootComments(1L, PageRequest.of(0, 20), null))
                .isInstanceOf(OpinionException.class);
    }

    @Test
    @DisplayName("원댓글 목록을 조회하면 답글은 5개까지 미리보기로, 6개 이상이면 hasMoreReplies가 true로 반환된다")
    void getRootCommentsGroupsRepliesWithPreviewAndCount() {
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        Comment root1 = rootComment(1L, opinion, writer);
        Comment root2 = rootComment(2L, opinion, writer);
        Pageable pageable = PageRequest.of(0, 20);
        given(commentQueryRepository.findRootComments(1L, pageable, null))
                .willReturn(new PageImpl<>(List.of(root1, root2), pageable, 2));

        List<Comment> root1Preview = List.of(
                replyComment(11L, root1, writer), replyComment(12L, root1, writer),
                replyComment(13L, root1, writer), replyComment(14L, root1, writer),
                replyComment(15L, root1, writer));
        given(commentQueryRepository.countRepliesByParentIds(List.of(1L, 2L), null))
                .willReturn(Map.of(1L, 6L));
        given(commentQueryRepository.findReplyPreviewsByParentIds(List.of(1L, 2L), 5, null))
                .willReturn(Map.of(1L, root1Preview));

        Page<RootCommentGroup> results = commentService.getRootComments(1L, pageable, null);

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
    @DisplayName("로그인 사용자가 조회하면 차단 필터링을 위해 currentUserId를 조회 리포지토리에 그대로 전달한다")
    void getRootCommentsPassesCurrentUserIdForBlockFiltering() {
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        Pageable pageable = PageRequest.of(0, 20);
        given(commentQueryRepository.findRootComments(1L, pageable, 30L))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));
        given(commentQueryRepository.countRepliesByParentIds(List.of(), 30L)).willReturn(Map.of());
        given(commentQueryRepository.findReplyPreviewsByParentIds(List.of(), 5, 30L)).willReturn(Map.of());

        commentService.getRootComments(1L, pageable, 30L);

        org.mockito.Mockito.verify(commentQueryRepository).findRootComments(1L, pageable, 30L);
    }

    @Test
    @DisplayName("존재하지 않는 원댓글의 답글을 조회하면 예외가 발생한다")
    void getRepliesFailsWhenParentCommentNotFound() {
        given(commentRepository.findByIdWithUser(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getReplies(999L, PageRequest.of(0, 20), null))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("답글 더보기를 요청하면 해당 원댓글의 답글을 페이지네이션으로 반환한다")
    void getRepliesReturnsPageFromQueryRepository() {
        Comment root = rootComment(1L, opinion, writer);
        given(commentRepository.findByIdWithUser(1L)).willReturn(Optional.of(root));
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        Pageable pageable = PageRequest.of(0, 5);
        Page<Comment> expected = new PageImpl<>(List.of(replyComment(11L, root, writer)), pageable, 1);
        given(commentQueryRepository.findReplies(1L, pageable, null)).willReturn(expected);

        Page<Comment> results = commentService.getReplies(1L, pageable, null);

        assertThat(results).isEqualTo(expected);
    }

    @Test
    @DisplayName("삭제된 흔적의 답글을 조회하면 예외가 발생한다")
    void getRepliesFailsWhenOpinionDeleted() {
        Comment root = rootComment(1L, opinion, writer);
        given(commentRepository.findByIdWithUser(1L)).willReturn(Optional.of(root));
        opinion.delete();
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));

        assertThatThrownBy(() -> commentService.getReplies(1L, PageRequest.of(0, 20), null))
                .isInstanceOf(OpinionException.class);
    }

    @Test
    @DisplayName("원댓글을 작성하면 부모 댓글 없이 저장된다")
    void createRootComment() {
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(userRepository.findById(10L)).willReturn(Optional.of(writer));
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> invocation.getArgument(0));

        Comment created = commentService.createComment(1L, 10L, new CreateCommentRequest(null, "내용"));

        assertThat(created.getParentComment()).isNull();
        assertThat(created.getContent()).isEqualTo("내용");
        assertThat(created.getUser()).isEqualTo(writer);
        org.mockito.Mockito.verify(eventPublisher).publishEvent(any(CommentCreatedEvent.class));
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
        given(commentRepository.findByIdWithUser(1L)).willReturn(Optional.of(root));
        given(userRepository.findById(20L)).willReturn(Optional.of(otherUser));
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> invocation.getArgument(0));

        Comment created = commentService.createComment(1L, 20L, new CreateCommentRequest(1L, "답글 내용"));

        assertThat(created.getParentComment()).isEqualTo(root);
        assertThat(created.getContent()).isEqualTo("답글 내용");
    }

    @Test
    @DisplayName("존재하지 않는 부모 댓글에 답글을 작성하면 예외가 발생한다")
    void createReplyFailsWhenParentNotFound() {
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(userRepository.findById(10L)).willReturn(Optional.of(writer));
        given(commentRepository.findByIdWithUser(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(1L, 10L, new CreateCommentRequest(999L, "내용")))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("다른 흔적에 속한 댓글을 부모로 답글을 작성하면 예외가 발생한다")
    void createReplyFailsWhenParentBelongsToDifferentOpinion() {
        Opinion otherOpinion = opinion(2L);
        Comment parentOfOtherOpinion = rootComment(1L, otherOpinion, writer);
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(userRepository.findById(10L)).willReturn(Optional.of(writer));
        given(commentRepository.findByIdWithUser(1L)).willReturn(Optional.of(parentOfOtherOpinion));

        assertThatThrownBy(() -> commentService.createComment(1L, 10L, new CreateCommentRequest(1L, "내용")))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("삭제된 부모 댓글에 답글을 작성하면 예외가 발생한다")
    void createReplyFailsWhenParentIsDeleted() {
        Comment root = rootComment(1L, opinion, writer);
        root.delete();
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(userRepository.findById(10L)).willReturn(Optional.of(writer));
        given(commentRepository.findByIdWithUser(1L)).willReturn(Optional.of(root));

        assertThatThrownBy(() -> commentService.createComment(1L, 10L, new CreateCommentRequest(1L, "내용")))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("답글에 다시 답글을 작성하면 1-depth 제약 예외가 그대로 전파된다")
    void createReplyToReplyPropagatesNestedReplyException() {
        Comment root = rootComment(1L, opinion, writer);
        Comment reply = replyComment(2L, root, writer);
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(userRepository.findById(10L)).willReturn(Optional.of(writer));
        given(commentRepository.findByIdWithUser(2L)).willReturn(Optional.of(reply));

        assertThatThrownBy(() -> commentService.createComment(1L, 10L, new CreateCommentRequest(2L, "내용")))
                .isInstanceOf(NestedReplyNotAllowedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 수정하면 예외가 발생한다")
    void modifyCommentFailsWhenNotFound() {
        given(commentRepository.findByIdWithUser(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.modifyComment(999L, 10L, new UpdateCommentRequest("수정")))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("이미 삭제된 댓글을 수정하면 예외가 발생한다")
    void modifyCommentFailsWhenAlreadyDeleted() {
        Comment comment = rootComment(1L, opinion, writer);
        comment.delete();
        given(commentRepository.findByIdWithUser(1L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.modifyComment(1L, 10L, new UpdateCommentRequest("수정")))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("본인이 작성하지 않은 댓글을 수정하면 예외가 발생한다")
    void modifyCommentFailsWhenNotOwner() {
        Comment comment = rootComment(1L, opinion, writer);
        given(commentRepository.findByIdWithUser(1L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.modifyComment(1L, 20L, new UpdateCommentRequest("수정")))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("본인이 작성한 댓글을 수정하면 내용이 변경된다")
    void modifyCommentUpdatesContent() {
        Comment comment = rootComment(1L, opinion, writer);
        given(commentRepository.findByIdWithUser(1L)).willReturn(Optional.of(comment));

        Comment modified = commentService.modifyComment(1L, 10L, new UpdateCommentRequest("수정된 내용"));

        assertThat(modified.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("본인이 작성하지 않은 댓글을 삭제하면 예외가 발생한다")
    void removeCommentFailsWhenNotOwner() {
        Comment comment = rootComment(1L, opinion, writer);
        given(commentRepository.findByIdWithUser(1L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.removeComment(1L, 20L)).isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("본인이 작성한 댓글을 삭제하면 소프트 삭제된다")
    void removeCommentSoftDeletesComment() {
        Comment comment = rootComment(1L, opinion, writer);
        given(commentRepository.findByIdWithUser(1L)).willReturn(Optional.of(comment));

        commentService.removeComment(1L, 10L);

        assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("모임 전용 흔적의 댓글 목록을 모임원이 아닌 사용자가 조회하면 예외가 발생한다")
    void getRootCommentsFailsWhenNotGroupMember() {
        Group group = group(9L);
        Opinion groupOpinion = opinion(1L, group);
        given(opinionRepository.findById(1L)).willReturn(Optional.of(groupOpinion));
        doThrow(new GroupException(GroupErrorCode.NOT_MEMBER)).when(groupAccessValidator).validateMember(9L, 20L);

        assertThatThrownBy(() -> commentService.getRootComments(1L, PageRequest.of(0, 20), 20L))
                .isInstanceOf(GroupException.class);
        verify(commentQueryRepository, never()).findRootComments(any(), any(), any());
    }

    @Test
    @DisplayName("모임 전용 흔적의 답글을 모임원이 아닌 사용자가 조회하면 예외가 발생한다")
    void getRepliesFailsWhenNotGroupMember() {
        Group group = group(9L);
        Opinion groupOpinion = opinion(1L, group);
        Comment root = rootComment(1L, groupOpinion, writer);
        given(commentRepository.findByIdWithUser(1L)).willReturn(Optional.of(root));
        given(opinionRepository.findById(1L)).willReturn(Optional.of(groupOpinion));
        doThrow(new GroupException(GroupErrorCode.NOT_MEMBER)).when(groupAccessValidator).validateMember(9L, 20L);

        assertThatThrownBy(() -> commentService.getReplies(1L, PageRequest.of(0, 20), 20L))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("모임원이 아닌 사용자가 모임 전용 흔적에 댓글을 작성하려 하면 예외가 발생한다")
    void createCommentFailsWhenNotGroupMember() {
        Group group = group(9L);
        Opinion groupOpinion = opinion(1L, group);
        given(opinionRepository.findById(1L)).willReturn(Optional.of(groupOpinion));
        doThrow(new GroupException(GroupErrorCode.NOT_MEMBER)).when(groupAccessValidator).validateMember(9L, 20L);

        assertThatThrownBy(() -> commentService.createComment(1L, 20L, new CreateCommentRequest(null, "내용")))
                .isInstanceOf(GroupException.class);
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("모임원이 모임 전용 흔적에 댓글을 작성하면 정상적으로 저장된다")
    void createCommentSucceedsWhenGroupMember() {
        Group group = group(9L);
        Opinion groupOpinion = opinion(1L, group);
        given(opinionRepository.findById(1L)).willReturn(Optional.of(groupOpinion));
        given(userRepository.findById(10L)).willReturn(Optional.of(writer));
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> invocation.getArgument(0));

        Comment created = commentService.createComment(1L, 10L, new CreateCommentRequest(null, "내용"));

        assertThat(created.getContent()).isEqualTo("내용");
        verify(groupAccessValidator).validateMember(9L, 10L);
    }
}
