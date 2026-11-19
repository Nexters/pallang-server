package com.nexters.palang.global.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.global.common.error.AppException;
import com.nexters.palang.global.common.error.GlobalErrorCode;
import com.nexters.palang.global.config.JpaAuditingConfig;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

// IdempotentExecutor는 트랜잭션을 직접 열고 닫아야 의미가 있으므로, 테스트 메서드를 감싸는 기본 트랜잭션을 끄고
// 실제 커밋/롤백이 일어나게 한다(그래서 데이터는 @AfterEach에서 직접 정리한다).
@DataJpaTest
@Import(JpaAuditingConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class IdempotentExecutorTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private IdempotentExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new IdempotentExecutor(idempotencyRecordRepository, new TransactionTemplate(transactionManager));
    }

    @AfterEach
    void tearDown() {
        idempotencyRecordRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 요청 키로 두 번 실행하면 작업은 한 번만 실행되고 두 번째는 처음 결과를 돌려준다")
    void sameKeyRunsActionOnce() {
        AtomicInteger calls = new AtomicInteger();

        Long first = executor.execute(USER_ID, IdempotentOperation.CREATE_GROUP, "key-1", () -> 100L + calls.incrementAndGet());
        Long second = executor.execute(USER_ID, IdempotentOperation.CREATE_GROUP, "key-1", () -> 100L + calls.incrementAndGet());

        assertThat(first).isEqualTo(101L);
        assertThat(second).isEqualTo(101L);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 사용자나 다른 키는 별개 요청으로 실행된다")
    void differentUserOrKeyRunsSeparately() {
        AtomicInteger calls = new AtomicInteger();

        executor.execute(USER_ID, IdempotentOperation.CREATE_GROUP, "key-1", () -> 100L + calls.incrementAndGet());
        executor.execute(USER_ID, IdempotentOperation.CREATE_GROUP, "key-2", () -> 100L + calls.incrementAndGet());
        executor.execute(2L, IdempotentOperation.CREATE_GROUP, "key-1", () -> 100L + calls.incrementAndGet());

        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("요청 키가 없으면 매번 실행하고 기록을 남기지 않는다")
    void nullKeyRunsEveryTime() {
        AtomicInteger calls = new AtomicInteger();

        executor.execute(USER_ID, IdempotentOperation.CREATE_GROUP, null, () -> 100L + calls.incrementAndGet());
        executor.execute(USER_ID, IdempotentOperation.CREATE_GROUP, null, () -> 100L + calls.incrementAndGet());

        assertThat(calls.get()).isEqualTo(2);
        assertThat(idempotencyRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("작업이 실패하면 기록이 남지 않아 같은 키로 다시 시도할 수 있다")
    void failedActionLeavesNoRecord() {
        assertThatThrownBy(() -> executor.execute(USER_ID, IdempotentOperation.CREATE_GROUP, "key-1", () -> {
            throw new IllegalStateException("실패");
        })).isInstanceOf(IllegalStateException.class);

        Long retried = executor.execute(USER_ID, IdempotentOperation.CREATE_GROUP, "key-1", () -> 200L);

        assertThat(retried).isEqualTo(200L);
    }

    @Test
    @DisplayName("같은 키가 동시에 들어와 먼저 커밋한 요청이 있으면, 늦은 요청은 롤백되고 먼저 커밋한 결과를 돌려준다")
    void concurrentSameKeyReturnsWinnerResult() {
        TransactionTemplate otherRequest = new TransactionTemplate(transactionManager);
        otherRequest.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // 늦은 요청의 작업이 실행되는 사이, 다른 요청이 같은 키로 먼저 커밋한 상황을 만든다.
        Long result = executor.execute(USER_ID, IdempotentOperation.CREATE_GROUP, "key-1", () -> {
            otherRequest.executeWithoutResult(status -> idempotencyRecordRepository.saveAndFlush(
                    IdempotencyRecord.of(USER_ID, IdempotentOperation.CREATE_GROUP, "key-1", 100L)));
            return 999L;
        });

        assertThat(result).isEqualTo(100L);
        assertThat(idempotencyRecordRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("요청 키가 비었거나 100자를 넘으면 예외가 발생한다")
    void rejectsInvalidKey() {
        assertThatThrownBy(() -> executor.execute(USER_ID, IdempotentOperation.CREATE_GROUP, " ", () -> 1L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(GlobalErrorCode.INVALID_IDEMPOTENCY_KEY);
        assertThatThrownBy(() -> executor.execute(USER_ID, IdempotentOperation.CREATE_GROUP, "a".repeat(101), () -> 1L))
                .isInstanceOf(AppException.class);
    }
}
