package com.nexters.palang.domain.book.domain;

import com.nexters.palang.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "books", indexes = @Index(name = "idx_books_title_normalized", columnList = "title_normalized"))
@Check(constraints = "page_count > 0")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    // 검색/자동완성 접두어 매칭용으로 title에서 공백 제거 + 소문자 변환해 미리 저장해둔 컬럼.
    // 매번 title에 replace()/lower() 함수를 걸어 비교하는 대신 이 컬럼(인덱스 적용)을 비교한다.
    // ddl-auto: update로 기존 테이블에 추가되므로 nullable로 두고, @PrePersist/@PreUpdate에서
    // 항상 채운다. 이미 저장된 기존 row는 이 배포 시점에 별도 백필(backfill)이 필요하다.
    @Column(name = "title_normalized")
    private String titleNormalized;

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

    @PrePersist
    @PreUpdate
    private void normalizeTitle() {
        this.titleNormalized = normalize(title);
    }

    public static String normalize(String value) {
        return value == null ? "" : value.replace(" ", "").toLowerCase();
    }
}
