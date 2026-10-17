package com.nexters.palang.domain.notification.infrastructure;

import com.nexters.palang.domain.notification.domain.Notification;
import com.nexters.palang.domain.notification.domain.NotificationType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    // 알림함 "활동" 탭: 모임에 속하지 않은(group_id IS NULL) 알림만.
    Page<Notification> findByReceiverIdAndGroupIdIsNullOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    // 알림함 "모임" 탭: 모임에 속한(group_id IS NOT NULL) 알림만.
    Page<Notification> findByReceiverIdAndGroupIdIsNotNullOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    // BookNewOpinionsNotifier: 이 사용자에게 이 책에 대해 마지막으로 보낸 BOOK_NEW_OPINIONS 알림의 snapshot을 조회.
    Optional<Notification> findFirstByReceiverIdAndTypeAndBookIdOrderByCreatedAtDesc(
            Long receiverId, NotificationType type, Long bookId);

    // GroupEndingSoonNotifier: 스케줄러가 같은 날 여러 번(재시도 등) 돌아도 같은 모임·사용자에게
    // GROUP_ENDING_SOON을 중복 발송하지 않도록 "오늘 이미 보냈는지"만 확인한다. 날짜 범위로 제한해야
    // 모임의 종료일이 나중에 다시 바뀌어 임박 알림을 또 보내야 하는 경우를 영구히 막지 않는다.
    boolean existsByReceiverIdAndTypeAndGroupIdAndCreatedAtAfter(
            Long receiverId, NotificationType type, Long groupId, LocalDateTime createdAtAfter);

    @Modifying
    @Query("update Notification n set n.read = true, n.readAt = current_timestamp "
            + "where n.receiver.id = :receiverId and n.read = false")
    int markAllAsRead(@Param("receiverId") Long receiverId);

    // 관리자 유저 삭제(AdminUserService): 이 유저가 수신자인 알림 전부. actorId(알림을 유발한 유저)가
    // 이 유저를 가리키는 다른 사람의 알림은 FK 제약이 없어 그대로 남는다(알려진 한계, 이슈 #155 참고).
    void deleteAllByReceiverId(Long receiverId);
}
