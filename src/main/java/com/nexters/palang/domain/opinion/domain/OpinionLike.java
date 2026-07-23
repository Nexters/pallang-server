package com.nexters.palang.domain.opinion.domain;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Entity
@Table(
        name = "opinion_likes",
        uniqueConstraints = @UniqueConstraint(name = "uq_likes_user_opinion", columnNames = {"user_id", "opinion_id"}),
        indexes = {
                @Index(name = "idx_likes_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_likes_opinion", columnList = "opinion_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpinionLike extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opinion_id", nullable = false)
    private Opinion opinion;

    @Builder
    private OpinionLike(User user, Opinion opinion) {
        this.user = user;
        this.opinion = opinion;
    }
}
