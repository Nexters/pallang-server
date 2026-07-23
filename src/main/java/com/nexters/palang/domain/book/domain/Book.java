package com.nexters.palang.domain.book.domain;

import com.nexters.palang.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Entity
@Table(name = "books")
@Check(constraints = "page_count > 0")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "author", nullable = false)
    private String author;

    @Column(name = "publisher", nullable = false)
    private String publisher;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Column(name = "isbn", length = 20)
    private String isbn;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private BookSource source;

    @Builder
    private Book(
            String title,
            String author,
            String publisher,
            int pageCount,
            String isbn,
            String coverImageUrl,
            BookSource source
    ) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.pageCount = pageCount;
        this.isbn = isbn;
        this.coverImageUrl = coverImageUrl;
        this.source = source != null ? source : BookSource.API;
    }
}
