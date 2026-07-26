package com.nexters.palang.domain.opinion.domain;

import com.nexters.palang.domain.decoration.domain.Decoration;
import com.nexters.palang.domain.decoration.domain.DecorationRangeValidator;
import com.nexters.palang.global.common.entity.BaseEntity;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
                @Index(name = "idx_opinions_passage_sort", columnList = "passage_id, like_count, created_at"),
                // 최신순 정렬 전용 (idx_opinions_passage_sort는 좋아요순 전용)
                @Index(name = "idx_opinions_passage_created", columnList = "passage_id, created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Opinion extends BaseEntity {

    public static final int CONTENT_MAX_LENGTH = 500;

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

    @Column(name = "content", length = CONTENT_MAX_LENGTH, nullable = false)
    private String content;

    // OpinionLike 생성/삭제에 맞춰 OpinionLikeService가 증감시키는 캐시 값 (backend-plan.md §5.4)
    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "opinion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Decoration> decorations = new ArrayList<>();

    @Builder
    private Opinion(Passage passage, User user, String content) {
        this.passage = passage;
        this.user = user;
        this.content = content;
        this.likeCount = 0;
    }

    // 흔적 작성(직접 입력, FR-WRITE-10): 겹치지 않는 Decoration들과 함께 Opinion을 생성한다.
    // Decoration 겹침(Q-09/FR-WRITE-09)은 같은 Opinion 안의 형제 엔티티끼리만 비교하면 판단 가능한
    // 생성 시점 불변식이라 Comment.root()/reply()와 같은 방식으로 정적 팩토리에서 차단한다.
    public static Opinion createWithDecorations(Passage passage, User user, String content, List<Decoration> decorations) {
        DecorationRangeValidator.validate(decorations);
        Opinion opinion = Opinion.builder()
                .passage(passage)
                .user(user)
                .content(content)
                .build();
        decorations.forEach(opinion::addDecoration);
        return opinion;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

    public void addDecoration(Decoration decoration) {
        decoration.assignOpinion(this);
        this.decorations.add(decoration);
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
