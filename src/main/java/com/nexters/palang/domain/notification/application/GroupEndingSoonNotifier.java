package com.nexters.palang.domain.notification.application;

import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.notification.infrastructure.NotificationRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 모임 종료 하루 전 멤버 전원에게 알린다. 매일 한 번만 돌리면 되므로 당일 중복 발송 방지만
// 신경 쓴다(NotificationRepository.existsByReceiverIdAndTypeAndGroupIdAndCreatedAtAfter 참고) —
// 다음 날 다시 돌 때는 endDate가 달라져 있을 모임만 대상이 되므로 별도 snapshot은 필요 없다.
@Component
@RequiredArgsConstructor
public class GroupEndingSoonNotifier {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationCreationService notificationCreationService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void notifyGroupsEndingTomorrow() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Group> endingSoon = groupRepository.findAllByEndDate(tomorrow);
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        for (Group group : endingSoon) {
            String title = "모임 종료 하루 전";
            String body = "'%s' 모임이 내일 끝나요.".formatted(group.getName());
            List<Long> memberIds = groupMemberRepository.findUserIdsByGroupId(group.getId());
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
}
