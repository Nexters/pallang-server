package com.nexters.palang.domain.notification.application;

import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.ReadingStatusMode;
import com.nexters.palang.domain.group.infrastructure.GroupBookEntryRepository;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.notification.infrastructure.NotificationRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 모임에서 함께 읽는 책(GroupBookEntry)의 종료일 하루 전에 그 모임 멤버 전원에게 알린다. 기간은 이제 책마다
// 따로 있어(#223) 한 모임에서 같은 날 끝나는 책이 여러 권이어도 모임당 한 번만 보낸다. 직접 완독 처리한 책
// (MANUAL)은 종료일과 무관하게 상태가 정해졌으므로 대상에서 뺀다. 매일 한 번만 돌리면 되므로 당일 중복 발송
// 방지만 신경 쓴다(NotificationRepository.existsByReceiverIdAndTypeAndGroupIdAndCreatedAtAfter 참고) —
// 다음 날 다시 돌 때는 endDate가 달라져 있을 책만 대상이 되므로 별도 snapshot은 필요 없다.
@Component
@RequiredArgsConstructor
public class GroupEndingSoonNotifier {

    // 스케줄 트리거(@Scheduled zone)와 "오늘/내일" 날짜 계산 기준을 같은 시간대로 맞춘다.
    // JVM 기본 시간대(-Duser.timezone)에 암묵적으로 의존하면 그 설정이 바뀌거나 테스트처럼
    // 다른 시간대에서 실행될 때 하루 밀리는 버그가 생길 수 있다.
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final GroupBookEntryRepository groupBookEntryRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationCreationService notificationCreationService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void notifyGroupsEndingTomorrow() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate tomorrow = today.plusDays(1);
        Map<Long, List<GroupBookEntry>> endingSoonByGroupId = groupBookEntryRepository
                .findAllInActiveGroupsByEndDate(tomorrow).stream()
                .filter(entry -> entry.getStatusMode() == ReadingStatusMode.AUTO)
                .collect(Collectors.groupingBy(entry -> entry.getGroup().getId(), LinkedHashMap::new, Collectors.toList()));
        LocalDateTime startOfToday = today.atStartOfDay();

        for (List<GroupBookEntry> entries : endingSoonByGroupId.values()) {
            Group group = entries.getFirst().getGroup();
            String title = "함께 읽는 책 종료 하루 전";
            String body = endingSoonBody(group.getName(), entries);
            List<Long> memberIds = groupMemberRepository.findActiveUserIdsByGroupId(group.getId());
            for (Long memberId : memberIds) {
                boolean alreadyNotifiedToday = notificationRepository.existsByReceiverIdAndTypeAndGroupIdAndCreatedAtAfter(
                        memberId, NotificationType.GROUP_ENDING_SOON, group.getId(), startOfToday);
                if (alreadyNotifiedToday) {
                    continue;
                }
                notificationCreationService.create(memberId, null, NotificationType.GROUP_ENDING_SOON,
                        title, body, null, null, null, group.getId(), group.getName(), null);
            }
        }
    }

    // 모임은 여러 권을 읽으므로 "모임이 끝난다"가 아니라 "어떤 책 읽기가 끝난다"고 알린다. 같은 날 여러 권이 끝나면
    // 첫 책 제목과 나머지 권수로 묶는다.
    private static String endingSoonBody(String groupName, List<GroupBookEntry> entries) {
        String firstTitle = entries.getFirst().getBook().getTitle();
        if (entries.size() == 1) {
            return "'%s' 모임의 '%s' 읽기가 내일 끝나요.".formatted(groupName, firstTitle);
        }
        return "'%s' 모임의 '%s' 외 %d권 읽기가 내일 끝나요.".formatted(groupName, firstTitle, entries.size() - 1);
    }
}
