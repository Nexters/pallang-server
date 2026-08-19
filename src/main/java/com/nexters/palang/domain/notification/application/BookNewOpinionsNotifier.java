package com.nexters.palang.domain.notification.application;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.notification.infrastructure.NotificationRepository;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * "내가 의견 남긴 책에 새 의견 N개 이상" 알림의 집계/중복방지 로직.
 * 별도 캐시(Redis) 없이 Notification 테이블 자체를 상태 저장소로 재사용한다: 마지막으로 보낸
 * BOOK_NEW_OPINIONS 알림의 opinionCountSnapshot과 현재 의견 수의 차이가 threshold 이상이면 다시 알린다.
 */
@Component
public class BookNewOpinionsNotifier {

    private final OpinionRepository opinionRepository;
    private final NotificationRepository notificationRepository;
    private final BookRepository bookRepository;
    private final NotificationCreationService notificationCreationService;
    private final int threshold;

    public BookNewOpinionsNotifier(
            OpinionRepository opinionRepository,
            NotificationRepository notificationRepository,
            BookRepository bookRepository,
            NotificationCreationService notificationCreationService,
            @Value("${notification.book-new-opinions-threshold:5}") int threshold
    ) {
        this.opinionRepository = opinionRepository;
        this.notificationRepository = notificationRepository;
        this.bookRepository = bookRepository;
        this.notificationCreationService = notificationCreationService;
        this.threshold = threshold;
    }

    @Transactional
    public void notifyIfThresholdReached(Long bookId, Long actorUserId) {
        long currentCount = opinionRepository.countByPassage_Book_IdAndDeletedAtIsNull(bookId);
        List<Long> candidateReceiverIds = opinionRepository.findDistinctUserIdsByBookId(bookId);
        if (candidateReceiverIds.isEmpty()) {
            return;
        }

        Book book = bookRepository.findById(bookId).orElse(null);
        String bookTitle = book != null ? book.getTitle() : "관심 도서";
        String title = "새로운 의견 소식";
        String body = "'%s'에 새로운 의견이 %d개 이상 달렸어요.".formatted(bookTitle, threshold);

        for (Long receiverId : candidateReceiverIds) {
            if (receiverId.equals(actorUserId)) {
                continue;
            }
            int lastSnapshot = notificationRepository
                    .findFirstByReceiverIdAndTypeAndBookIdOrderByCreatedAtDesc(
                            receiverId, NotificationType.BOOK_NEW_OPINIONS, bookId)
                    .map(n -> n.getOpinionCountSnapshot() == null ? 0 : n.getOpinionCountSnapshot())
                    .orElse(0);

            if (currentCount - lastSnapshot >= threshold) {
                notificationCreationService.create(receiverId, null, NotificationType.BOOK_NEW_OPINIONS,
                        title, body, null, bookId, null, (int) currentCount);
            }
        }
    }
}
