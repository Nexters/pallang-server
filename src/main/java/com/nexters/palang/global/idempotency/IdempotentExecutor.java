package com.nexters.palang.global.idempotency;

import com.nexters.palang.global.common.error.AppException;
import com.nexters.palang.global.common.error.GlobalErrorCode;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

// 요청 키가 있는 쓰기 작업을 "한 번만" 실행한다.
// 1) 같은 (사용자, 작업, 키)로 이미 처리한 결과가 있으면 action을 실행하지 않고 그 리소스 id를 돌려준다.
// 2) 없으면 action과 결과 기록 저장을 한 트랜잭션으로 묶어 실행한다 — 작업이 실패하면 기록도 남지 않는다.
// 3) 같은 키가 동시에 들어와 유니크 제약에 걸리면(먼저 커밋한 쪽이 이김) 진 쪽의 트랜잭션은 전부 롤백되고,
//    이긴 쪽의 결과를 새 트랜잭션에서 다시 조회해 돌려준다. 그래서 호출 측은 트랜잭션 없이 이 메서드를 불러야 한다
//    (바깥 트랜잭션이 있으면 거기에 참여하므로 3)의 재조회는 동작하지 않는다).
// 키가 없으면(null) 기존처럼 매번 실행한다.
@Component
@RequiredArgsConstructor
public class IdempotentExecutor {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final TransactionTemplate transactionTemplate;

    public Long execute(Long userId, IdempotentOperation operation, String requestKey, Supplier<Long> action) {
        if (requestKey == null) {
            return transactionTemplate.execute(status -> action.get());
        }
        validateRequestKey(requestKey);
        try {
            return transactionTemplate.execute(status -> findResourceId(userId, operation, requestKey)
                    .orElseGet(() -> {
                        Long resourceId = action.get();
                        idempotencyRecordRepository.saveAndFlush(
                                IdempotencyRecord.of(userId, operation, requestKey, resourceId));
                        return resourceId;
                    }));
        } catch (DataIntegrityViolationException e) {
            return transactionTemplate.execute(status -> findResourceId(userId, operation, requestKey)
                    .orElseThrow(() -> e));
        }
    }

    private Optional<Long> findResourceId(Long userId, IdempotentOperation operation, String requestKey) {
        return idempotencyRecordRepository
                .findByUserIdAndOperationAndRequestKey(userId, operation.name(), requestKey)
                .map(IdempotencyRecord::getResourceId);
    }

    private void validateRequestKey(String requestKey) {
        if (requestKey.isBlank() || requestKey.length() > IdempotencyRecord.REQUEST_KEY_MAX_LENGTH) {
            throw new AppException(GlobalErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
    }
}
