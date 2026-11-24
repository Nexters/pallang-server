package com.nexters.palang.domain.group.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.GroupInvitation;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberRemovalReason;
import com.nexters.palang.domain.group.domain.GroupMemberRole;
import com.nexters.palang.domain.group.domain.GroupMemberStatus;
import com.nexters.palang.domain.group.domain.ReadingPeriod;
import com.nexters.palang.domain.group.infrastructure.GroupBookEntryRepository;
import com.nexters.palang.domain.group.infrastructure.GroupInvitationRepository;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRemovalRepository;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
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

// 내보내기의 전체 흐름(F04)을 실제 API로 확인한다: 내보내기 → 명단/인원 갱신 → 내보내진 사람의 접근 차단 →
// 예전 초대로 재가입 불가 → 내보낸 뒤 재발급한 초대로는 재가입 가능.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GroupMemberRemovalIntegrationTest {

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
    private GroupBookEntryRepository groupBookEntryRepository;
    @Autowired
    private GroupInvitationRepository groupInvitationRepository;
    @Autowired
    private GroupMemberRemovalRepository groupMemberRemovalRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Long hostId;
    private Long memberId;
    private Long groupId;
    private String originalInviteCode;

    @BeforeEach
    void setUp() {
        Book book = bookRepository.save(Book.builder()
                .title("프랑켄슈타인").author("메리 셸리").publisher("문학동네").pageCount(300).build());
        User host = userRepository.save(User.builder()
                .nickname("모임장").snsProvider(SnsProvider.KAKAO).snsId("removal-host")
                .termsAgreedAt(LocalDateTime.now()).build());
        User member = userRepository.save(User.builder()
                .nickname("참여자").snsProvider(SnsProvider.KAKAO).snsId("removal-member")
                .termsAgreedAt(LocalDateTime.now()).build());
        Group group = groupRepository.save(Group.create("고전 뽀개기", host, 4));
        groupMemberRepository.save(GroupMember.join(group, host, GroupMemberRole.HOST, LocalDateTime.now()));
        groupBookEntryRepository.save(GroupBookEntry.create(group, book, ReadingPeriod.NONE, LocalDate.now()));
        originalInviteCode = groupInvitationRepository.save(GroupInvitation.issue(group, host.getId())).getCode();

        hostId = host.getId();
        memberId = member.getId();
        groupId = group.getId();
    }

    private String bearer(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

    private void join(String inviteCode, Long userId, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/groups/invitations/{code}/join", inviteCode).header("Authorization", bearer(userId)))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    @DisplayName("내보낸 멤버는 명단과 접근권에서 빠지고, 예전 초대로는 못 돌아오며, 재발급한 초대로는 돌아올 수 있다")
    void removalFlow() throws Exception {
        join(originalInviteCode, memberId, 200);

        // 내보내기 → 최신 명단(모임장만 남음)
        mockMvc.perform(post("/api/groups/{groupId}/members/{userId}/removal", groupId, memberId)
                        .header("Authorization", bearer(hostId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reasonCode\":\"SPAM_OR_OFF_TOPIC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.members[0].userId").value(hostId));

        assertThat(groupMemberRepository.findByGroupIdAndUserId(groupId, memberId).orElseThrow().getStatus())
                .isEqualTo(GroupMemberStatus.REMOVED);
        assertThat(groupMemberRemovalRepository.findAllByGroupIdAndTargetUserId(groupId, memberId))
                .singleElement()
                .satisfies(removal -> {
                    assertThat(removal.getActorUserId()).isEqualTo(hostId);
                    assertThat(removal.getReason()).isEqualTo(GroupMemberRemovalReason.SPAM_OR_OFF_TOPIC);
                });

        // 내보내진 사람은 모임을 더 볼 수 없다
        mockMvc.perform(get("/api/groups/{groupId}", groupId).header("Authorization", bearer(memberId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("GROUP_403_2"));
        mockMvc.perform(get("/api/groups/{groupId}/books", groupId).header("Authorization", bearer(memberId)))
                .andExpect(status().isForbidden());

        // 예전 초대로는 다시 들어올 수 없다
        mockMvc.perform(post("/api/groups/invitations/{code}/join", originalInviteCode)
                        .header("Authorization", bearer(memberId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("GROUP_403_3"));

        // 모임장이 내보낸 뒤 재발급한 초대로는 다시 들어올 수 있다
        String body = mockMvc.perform(post("/api/groups/{groupId}/invite-link/regenerate", groupId)
                        .header("Authorization", bearer(hostId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(body).get("data");
        join(data.get("inviteCode").asText(), memberId, 200);

        assertThat(groupMemberRepository.existsActiveMember(groupId, memberId)).isTrue();
    }

    @Test
    @DisplayName("이미 내보낸 멤버를 다시 내보내도 이력이 한 번만 남는다")
    void removingTwiceRecordsOnce() throws Exception {
        join(originalInviteCode, memberId, 200);
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/groups/{groupId}/members/{userId}/removal", groupId, memberId)
                            .header("Authorization", bearer(hostId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reasonCode\":\"MEMBER_REQUEST_OR_MISTAKE\"}"))
                    .andExpect(status().isOk());
        }

        assertThat(groupMemberRemovalRepository.findAllByGroupIdAndTargetUserId(groupId, memberId)).hasSize(1);
    }
}
