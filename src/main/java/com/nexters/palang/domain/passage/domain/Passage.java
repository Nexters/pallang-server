package com.nexters.palang.domain.passage.domain;

import com.nexters.palang.domain.book.domain.Book;
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
                @Index(name = "idx_passages_book_hash", columnList = "book_id, normalized_hash")
        }
)
@Check(constraints = "page_number > 0")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Passage extends BaseEntity {

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

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "quoted_text", length = PassagePolicy.QUOTED_TEXT_MAX_LENGTH, nullable = false)
    private String quotedText;

    @Column(name = "is_spoiler", nullable = false)
    private boolean isSpoiler;

    // 유사 문장 판정(FR-WRITE-07)용: quotedText에서 공백·구두점 제거 후 정규화한 SHA-256 해시
    @Column(name = "normalized_hash", length = 64, nullable = false)
    private String normalizedHash;

    @Builder
    private Passage(Book book, User creator, int pageNumber, String quotedText, boolean isSpoiler, String normalizedHash) {
        this.book = book;
        this.creator = creator;
        this.pageNumber = pageNumber;
        this.quotedText = quotedText;
        this.isSpoiler = isSpoiler;
        this.normalizedHash = normalizedHash;
    }
}
