package com.nexters.palang.domain.decoration.domain;

import com.nexters.palang.global.common.entity.BaseEntity;
import com.nexters.palang.domain.opinion.domain.Opinion;
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
        name = "decorations",
        indexes = @Index(name = "idx_decorations_opinion", columnList = "opinion_id")
)
@Check(constraints = "start_offset >= 0 AND end_offset > start_offset")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Decoration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opinion_id", nullable = false)
    private Opinion opinion;

    @Column(name = "start_offset", nullable = false)
    private int startOffset;

    @Column(name = "end_offset", nullable = false)
    private int endOffset;

    @Enumerated(EnumType.STRING)
    @Column(name = "effect_type", length = 30, nullable = false)
    private EffectType effectType;

    @Column(name = "color", length = 20, nullable = false)
    private String color;

    @Builder
    private Decoration(Opinion opinion, int startOffset, int endOffset, EffectType effectType, String color) {
        this.opinion = opinion;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.effectType = effectType;
        this.color = color != null ? color : "#PRIMARY";
    }

    public void assignOpinion(Opinion opinion) {
        this.opinion = opinion;
    }
}
