package com.nexters.palang.domain.block.domain;

import com.nexters.palang.domain.block.common.BlockErrorCode;
import com.nexters.palang.domain.block.common.BlockException;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "user_blocks",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_blocks_blocker_blocked", columnNames = {"blocker_id", "blocked_id"}),
        indexes = {
                @Index(name = "idx_user_blocks_blocker", columnList = "blocker_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    private UserBlock(User blocker, User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
    }

    public static UserBlock of(User blocker, User blocked) {
        if (blocker.getId().equals(blocked.getId())) {
            throw new BlockException(BlockErrorCode.SELF_BLOCK_NOT_ALLOWED);
        }
        return new UserBlock(blocker, blocked);
    }
}
