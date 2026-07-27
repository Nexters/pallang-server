package com.nexters.palang.domain.policy.domain;

import com.nexters.palang.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "policies", uniqueConstraints = @UniqueConstraint(name = "uq_policies_type", columnNames = "type"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Policy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PolicyType type;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    private Policy(PolicyType type, String content) {
        this.type = type;
        this.content = content;
    }
}
