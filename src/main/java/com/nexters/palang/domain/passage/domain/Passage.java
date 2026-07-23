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
import org.hibernate.annotations.Check;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Entity
@Table(
        name = "passages",
        indexes = @Index(name = "idx_passages_book_page", columnList = "book_id, page_number")
)
@Check(constraints = "page_number > 0")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Passage extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User creator;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "quoted_text", length = 150, nullable = false)
    private String quotedText;

    @Column(name = "is_spoiler", nullable = false)
    private boolean isSpoiler;

    @Builder
    private Passage(Book book, User creator, int pageNumber, String quotedText, boolean isSpoiler) {
        this.book = book;
        this.creator = creator;
        this.pageNumber = pageNumber;
        this.quotedText = quotedText;
        this.isSpoiler = isSpoiler;
    }
}
