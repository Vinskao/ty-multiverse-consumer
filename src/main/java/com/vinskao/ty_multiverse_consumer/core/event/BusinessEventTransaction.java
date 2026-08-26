package com.vinskao.ty_multiverse_consumer.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

/**
 * 把業務寫入與 outbox 事件包進同一個 transaction 的輔助類別
 *
 * <p>這是 Transactional Outbox 的重點：業務異動與待發布事件要嘛一起成功、
 * 要嘛一起回滾，才不會出現「資料改了但沒有稽核事件」或反過來的情況。</p>
 *
 * <p><b>批次操作的例外</b>：{@code saveAllPeople} 這類刻意不加 {@code @Transactional}
 * 以允許多連線並發的批次寫入，無法納入單一 transaction。這類操作請改用
 * {@link #recordAfter} —— 事件在業務寫入成功「之後」以獨立 transaction 記錄，
 * 保證較弱（寫入成功但事件遺失的視窗存在），但不會破壞既有的並發設計。</p>
 *
 * @author TY Backend Team
 * @since 2026-08
 */
@Component
public class BusinessEventTransaction {

    private static final Logger logger = LoggerFactory.getLogger(BusinessEventTransaction.class);

    private final TransactionalOperator transactionalOperator;
    private final BusinessEventRecorder recorder;

    public BusinessEventTransaction(ReactiveTransactionManager transactionManager,
                                    BusinessEventRecorder recorder) {
        this.transactionalOperator = TransactionalOperator.create(transactionManager);
        this.recorder = recorder;
    }

    /**
     * 在同一個 transaction 內執行業務寫入並記錄 succeeded 事件
     *
     * @param work        業務寫入
     * @param operation   操作前綴，例如 {@code people.update}
     * @param aggregateType {@code people} 或 {@code weapon}
     * @param aggregateId 由結果取出 aggregate id
     * @param payload     由結果組出事件 payload（不得含機密）
     * @param requestId   來自 AsyncMessageDTO 的 requestId
     */
    public <T> Mono<T> withSucceededEvent(Mono<T> work,
                                          String operation,
                                          String aggregateType,
                                          Function<T, String> aggregateId,
                                          Function<T, Map<String, Object>> payload,
                                          String requestId) {
        return transactionalOperator.transactional(
                work.flatMap(result -> recorder
                        .recordSucceeded(operation, aggregateType, aggregateId.apply(result), requestId,
                                payload.apply(result))
                        .thenReturn(result)));
    }

    /**
     * 業務寫入沒有回傳值時的版本（例如 delete、delete-all）
     */
    public Mono<Void> withSucceededEvent(Mono<Void> work,
                                         String operation,
                                         String aggregateType,
                                         String aggregateId,
                                         Map<String, Object> payload,
                                         String requestId) {
        return transactionalOperator.transactional(
                work.then(recorder.recordSucceeded(operation, aggregateType, aggregateId, requestId, payload)));
    }

    /**
     * 業務寫入成功「之後」以獨立 transaction 記錄事件。
     *
     * <p>只用在無法納入單一 transaction 的批次寫入。</p>
     */
    public Mono<Void> recordAfter(String operation,
                                  String aggregateType,
                                  String aggregateId,
                                  String requestId,
                                  Map<String, Object> payload) {
        return transactionalOperator.transactional(
                recorder.recordSucceeded(operation, aggregateType, aggregateId, requestId, payload));
    }

    /**
     * 記錄 failed 事件
     *
     * <p>業務 transaction 此時已經回滾，因此 failed 事件必須另開 transaction 寫入。
     * 這裡吞掉所有例外——稽核寫入失敗不應該再蓋掉原本的業務錯誤。</p>
     */
    public void recordFailedQuietly(String operation,
                                    String aggregateType,
                                    String aggregateId,
                                    String requestId,
                                    Throwable error) {
        try {
            transactionalOperator
                    .transactional(recorder.recordFailed(operation, aggregateType, aggregateId, requestId, error))
                    .block();
        } catch (Exception e) {
            logger.error("❌ 無法記錄 failed 事件: operation={}, requestId={}, cause={}",
                    operation, requestId, e.getMessage());
        }
    }
}
