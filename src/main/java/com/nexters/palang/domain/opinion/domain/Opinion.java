package com.nexters.palang.domain.opinion.domain;

import com.nexters.palang.global.common.entity.BaseEntity;
import com.nexters.palang.domain.passage.domain.Passage;
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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "opinions",
        indexes = {
                @Index(name = "idx_opinions_user", columnList = "user_id"),
                // DB 정의는 like_count DESC, created_at DESC 복합 정렬이지만 @Index는 정렬 방향을 표현하지 못한다.
                @Index(name = "idx_opinions_passage_sort", columnList = "passage_id, like_count, created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Opinion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passage_id", nullable = false)
    private Passage passage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "content", length = 500, nullable = false)
    private String content;

    // opinion_likes 테이블에 대한 INSERT/DELETE 트리거로 DB에서 직접 동기화되는 캐시 값
    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Builder
    private Opinion(Passage passage, User user, String content) {
        this.passage = passage;
        this.user = user;
        this.content = content;
        this.likeCount = 0;
    }
}
