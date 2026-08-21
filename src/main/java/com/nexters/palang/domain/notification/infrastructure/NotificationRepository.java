package com.nexters.palang.domain.notification.infrastructure;

import com.nexters.palang.domain.notification.domain.Notification;
import com.nexters.palang.domain.notification.domain.NotificationType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    // BookNewOpinionsNotifier: 이 사용자에게 이 책에 대해 마지막으로 보낸 BOOK_NEW_OPINIONS 알림의 snapshot을 조회.
    Optional<Notification> findFirstByReceiverIdAndTypeAndBookIdOrderByCreatedAtDesc(
            Long receiverId, NotificationType type, Long bookId);

    @Modifying
    @Query("update Notification n set n.read = true, n.readAt = current_timestamp "
            + "where n.receiver.id = :receiverId and n.read = false")
    int markAllAsRead(@Param("receiverId") Long receiverId);
}
