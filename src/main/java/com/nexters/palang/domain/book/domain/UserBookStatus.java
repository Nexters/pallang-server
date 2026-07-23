package com.nexters.palang.domain.book.domain;

import com.nexters.palang.global.common.entity.BaseEntity;
import com.nexters.palang.domain.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "user_book_statuses",
        uniqueConstraints = @UniqueConstraint(name = "uq_ubs_user_book", columnNames = {"user_id", "book_id"}),
        indexes = @Index(name = "idx_ubs_book", columnList = "book_id")
)
@Check(constraints = "current_page >= 0")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBookStatus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReadingStatus status;

    @Column(name = "current_page")
    private Integer currentPage;

    @Builder
    private UserBookStatus(User user, Book book, ReadingStatus status, Integer currentPage) {
        this.user = user;
        this.book = book;
        this.status = status;
        this.currentPage = currentPage;
    }

    public void updateStatus(ReadingStatus status) {
        this.status = status;
    }

    public void updateCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }
}
