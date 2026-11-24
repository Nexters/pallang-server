package com.nexters.palang.global.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 요청 키(Idempotency-Key)로 한 번 처리한 쓰기 요청의 결과(만들어진 리소스 id)를 기억한다. 응답 유실/이중 탭으로
// 같은 키가 다시 오면 새로 만들지 않고 이 결과를 돌려준다. (user_id, operation, request_key) 유니크 제약이
// 동시에 들어온 같은 키 요청 중 하나만 성공하게 한다.
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_idempotency_records_user_operation_key",
                columnNames = {"user_id", "operation", "request_key"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord {

    public static final int REQUEST_KEY_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    // 회원 삭제와 FK로 얽히지 않도록 연관관계 없이 값만 둔다.
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "operation", length = 50, nullable = false, updatable = false)
    private String operation;

    @Column(name = "request_key", length = REQUEST_KEY_MAX_LENGTH, nullable = false, updatable = false)
    private String requestKey;

    @Column(name = "resource_id", nullable = false, updatable = false)
    private Long resourceId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private IdempotencyRecord(Long userId, String operation, String requestKey, Long resourceId) {
        this.userId = userId;
        this.operation = operation;
        this.requestKey = requestKey;
        this.resourceId = resourceId;
    }

    public static IdempotencyRecord of(Long userId, IdempotentOperation operation, String requestKey, Long resourceId) {
        return new IdempotencyRecord(userId, operation.name(), requestKey, resourceId);
    }
}
