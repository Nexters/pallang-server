package com.nexters.palang.domain.passage.domain;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.global.common.entity.BaseEntity;
import com.nexters.palang.domain.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import org.hibernate.annotations.Check;

@Getter
@Entity
@Table(
        name = "passages",
        indexes = {
                @Index(name = "idx_passages_book_page", columnList = "book_id, page_number"),
                // group_id를 포함하도록 확장(구 idx_passages_book_hash 대체): 유사 문장 판정(FR-WRITE-07)이
                // 이제 책 전체가 아니라 (책, 모임) 단위로 이루어진다 — group_id가 NULL인 전역 공개 대목과
                // 특정 모임 전용 대목은 서로의 중복 판정에 관여하지 않는다.
                @Index(name = "idx_passages_book_group_hash", columnList = "book_id, group_id, normalized_hash")
        }
)
@Check(constraints = "page_number > 0")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Passage extends BaseEntity {

    public static final int QUOTED_TEXT_MAX_LENGTH = 150;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User creator;

    // 이 대목이 특정 모임 전용인지 표시한다. NULL이면 기존처럼 도서를 읽는 모든 사용자에게 전역 공개되고,
    // 값이 있으면 그 모임의 멤버만 조회/작성할 수 있다(OpinionService/PassageService의 GroupAccessValidator 참고).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "quoted_text", length = QUOTED_TEXT_MAX_LENGTH, nullable = false)
    private String quotedText;

    @Column(name = "is_spoiler", nullable = false)
    private boolean isSpoiler;

    // 유사 문장 판정(FR-WRITE-07)용: quotedText에서 공백·구두점 제거 후 정규화한 SHA-256 해시
    @Column(name = "normalized_hash", length = 64, nullable = false)
    private String normalizedHash;

    // 대목에 달린 마지막 흔적이 삭제되면(OpinionService) 대목도 함께 소프트 삭제된다 (PM 요구사항).
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Passage(
            Book book, User creator, Group group, int pageNumber, String quotedText, boolean isSpoiler, String normalizedHash) {
        this.book = book;
        this.creator = creator;
        this.group = group;
        this.pageNumber = pageNumber;
        this.quotedText = quotedText;
        this.isSpoiler = isSpoiler;
        this.normalizedHash = normalizedHash;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
