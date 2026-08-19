package com.nexters.palang.domain.group.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberRole;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.group.presentation.dto.CreateGroupRequest;
import com.nexters.palang.domain.group.presentation.dto.UpdateGroupRequest;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import com.nexters.palang.global.security.jwt.JwtTokenProvider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// open-in-view: false 환경에서 GroupMapper가 Group.book/host(둘 다 LAZY)를 컨트롤러 단에서 참조해
// LazyInitializationException(500)이 나는지 실제 트랜잭션 경계를 넘는 end-to-end 테스트로 검증한다.
// (CommentLazyLoadingIntegrationTest와 같은 패턴 — GroupRepository의 join fetch 쿼리
// (findByIdWithBookAndHost/findByIdForUpdate/findByInviteCodeForUpdate/findByInviteCode)가 없으면
// 이 테스트들이 실패한다.)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GroupLazyLoadingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Long bookId;
    private Long hostId;
    private Long otherId;

    @BeforeEach
    void setUp() {
        Book book = bookRepository.save(Book.builder()
                .title("프랑켄슈타인").author("메리 셸리").publisher("문학동네").pageCount(300).build());
        User host = userRepository.save(User.builder()
                .nickname("모임장").snsProvider(SnsProvider.KAKAO).snsId("host-1")
                .termsAgreedAt(LocalDateTime.now()).build());
        User other = userRepository.save(User.builder()
                .nickname("참여자").snsProvider(SnsProvider.KAKAO).snsId("other-1")
                .termsAgreedAt(LocalDateTime.now()).build());

        bookId = book.getId();
        hostId = host.getId();
        otherId = other.getId();
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

    private Group createGroupDirectly() {
        Book book = bookRepository.findById(bookId).orElseThrow();
        User host = userRepository.findById(hostId).orElseThrow();
        Group group = groupRepository.save(Group.create(
                "고전 뽀개기", book, host, 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20)));
        groupMemberRepository.save(GroupMember.of(group, host, GroupMemberRole.HOST));
        return group;
    }

    @Test
    @DisplayName("모임을 생성하면 host 닉네임과 책 제목이 포함된 응답이 정상적으로 내려온다")
    void createGroupReturnsHostNicknameAndBookTitle() throws Exception {
        CreateGroupRequest request = new CreateGroupRequest(
                "고전 뽀개기", bookId, 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));

        mockMvc.perform(post("/api/groups")
                        .header("Authorization", bearerToken(hostId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hostNickname").value("모임장"))
                .andExpect(jsonPath("$.data.bookTitle").value("프랑켄슈타인"));
    }

    @Test
    @DisplayName("모임 상세를 조회하면 host 닉네임과 책 제목이 포함된 응답이 정상적으로 내려온다")
    void getGroupDetailReturnsHostNicknameAndBookTitle() throws Exception {
        Group group = createGroupDirectly();

        mockMvc.perform(get("/api/groups/{groupId}", group.getId())
                        .header("Authorization", bearerToken(hostId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hostNickname").value("모임장"))
                .andExpect(jsonPath("$.data.bookTitle").value("프랑켄슈타인"));
    }

    @Test
    @DisplayName("방 설정을 변경하면 host 닉네임과 책 제목이 포함된 응답이 정상적으로 내려온다")
    void updateGroupReturnsHostNicknameAndBookTitle() throws Exception {
        Group group = createGroupDirectly();
        UpdateGroupRequest request = new UpdateGroupRequest(
                "주말 독서 모임", 6, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 27));

        mockMvc.perform(patch("/api/groups/{groupId}", group.getId())
                        .header("Authorization", bearerToken(hostId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hostNickname").value("모임장"))
                .andExpect(jsonPath("$.data.bookTitle").value("프랑켄슈타인"));
    }

    @Test
    @DisplayName("초대 링크로 가입하면 host 닉네임과 책 제목이 포함된 응답이 정상적으로 내려온다")
    void joinGroupReturnsHostNicknameAndBookTitle() throws Exception {
        Group group = createGroupDirectly();

        mockMvc.perform(post("/api/groups/invitations/{inviteCode}/join", group.getInviteCode())
                        .header("Authorization", bearerToken(otherId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hostNickname").value("모임장"))
                .andExpect(jsonPath("$.data.bookTitle").value("프랑켄슈타인"));
    }

    @Test
    @DisplayName("초대 링크를 미리보기하면 책 제목이 포함된 응답이 정상적으로 내려온다")
    void previewInvitationReturnsBookTitle() throws Exception {
        Group group = createGroupDirectly();

        mockMvc.perform(get("/api/groups/invitations/{inviteCode}", group.getInviteCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookTitle").value("프랑켄슈타인"));
    }
}
