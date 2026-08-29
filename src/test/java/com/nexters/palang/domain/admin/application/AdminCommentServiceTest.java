package com.nexters.palang.domain.admin.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.comment.common.CommentException;
import com.nexters.palang.domain.comment.domain.Comment;
import com.nexters.palang.domain.comment.infrastructure.CommentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminCommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    private AdminCommentService adminCommentService;

    @BeforeEach
    void setUp() {
        adminCommentService = new AdminCommentService(commentRepository);
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 삭제하려 하면 예외가 발생한다")
    void deleteFailsWhenCommentNotFound() {
        given(commentRepository.findWithAssociationsById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminCommentService.deleteComment(1L)).isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("댓글을 삭제하면 답글을 먼저 지운 뒤 본인을 지운다")
    void deleteCommentRemovesRepliesBeforeItself() {
        given(commentRepository.findWithAssociationsById(1L)).willReturn(Optional.of(mock(Comment.class)));

        adminCommentService.deleteComment(1L);

        InOrder inOrder = Mockito.inOrder(commentRepository);
        inOrder.verify(commentRepository).deleteAllByParentCommentIdIn(List.of(1L));
        inOrder.verify(commentRepository).deleteById(1L);
    }
}
