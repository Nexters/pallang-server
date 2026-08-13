package com.nexters.palang.domain.block.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexters.palang.domain.block.application.BlockService;
import com.nexters.palang.domain.block.common.BlockErrorCode;
import com.nexters.palang.domain.block.common.BlockException;
import com.nexters.palang.domain.block.domain.UserBlock;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.security.CurrentUserProvider;
import com.nexters.palang.global.security.LoginRequiredException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BlockController.class)
@AutoConfigureMockMvc(addFilters = false)
class BlockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlockService blockService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 20);

    private User user(Long id) {
        User built = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private UserBlock userBlock(User blocker, User blocked) {
        return UserBlock.of(blocker, blocked);
    }

    @Test
    @DisplayName("사용자를 차단하면 서비스에 차단을 위임한다")
    void block() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(post("/api/users/2/block")).andExpect(status().isOk());

        verify(blockService).block(1L, 2L);
    }

    @Test
    @DisplayName("인증 없이 차단을 시도하면 401 에러가 발생한다")
    void blockFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(post("/api/users/2/block"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("본인을 차단하려 하면 400 에러가 발생한다")
    void blockFailsWhenSelfBlock() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        doThrow(new BlockException(BlockErrorCode.SELF_BLOCK_NOT_ALLOWED)).when(blockService).block(1L, 1L);

        mockMvc.perform(post("/api/users/1/block"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("BLOCK_400_1"));
    }

    @Test
    @DisplayName("이미 차단한 사용자를 다시 차단하면 409 에러가 발생한다")
    void blockFailsWhenAlreadyBlocked() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        doThrow(new BlockException(BlockErrorCode.ALREADY_BLOCKED)).when(blockService).block(1L, 2L);

        mockMvc.perform(post("/api/users/2/block"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("BLOCK_409_1"));
    }

    @Test
    @DisplayName("차단을 해제하면 서비스에 해제를 위임한다")
    void unblock() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(delete("/api/users/2/block")).andExpect(status().isOk());

        verify(blockService).unblock(1L, 2L);
    }

    @Test
    @DisplayName("차단 내역이 없으면 차단 해제 시 404 에러가 발생한다")
    void unblockFailsWhenNotFound() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        doThrow(new BlockException(BlockErrorCode.BLOCK_NOT_FOUND)).when(blockService).unblock(1L, 2L);

        mockMvc.perform(delete("/api/users/2/block"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("BLOCK_404_1"));
    }

    @Test
    @DisplayName("차단 목록을 조회하면 차단한 사용자 목록을 반환한다")
    void getBlockedUsers() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        UserBlock block = userBlock(user(1L), user(2L));
        given(blockService.getBlockedUsers(eq(1L), any())).willReturn(new PageImpl<>(List.of(block), DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/users/me/blocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.users[0].userId").value(2));
    }
}
