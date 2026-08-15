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
import java.util.Locale;
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
    // 항상 채운다. 이미 저장된 기존 row는 이 컬럼을 갱신할 계기(@PreUpdate)가 없어 NULL로 남는데,
    // BookTitleNormalizationBackfillRunner가 앱 시작 시 자동으로 채워준다.
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

    // title_normalized 컬럼이 추가되기 전부터 존재하던 도서는 title이 바뀔 계기가 없어 이 컬럼이
    // 채워지지 않는다. BookTitleNormalizationBackfillRunner가 앱 시작 시 1회성으로 호출한다.
    public void backfillTitleNormalizedIfMissing() {
        if (this.titleNormalized == null) {
            this.titleNormalized = normalize(title);
        }
    }

    // toLowerCase()를 로케일 없이 쓰면 JVM 기본 로케일(터키어 등)에 따라 결과가 달라질 수 있어
    // (예: "I".toLowerCase() != "i") Locale.ROOT로 고정한다. 저장 시점과 검색 시점의 결과가
    // 항상 같아야 하므로 로케일 의존성을 없애는 게 중요하다.
    public static String normalize(String value) {
        return value == null ? "" : value.replace(" ", "").toLowerCase(Locale.ROOT);
    }
}
