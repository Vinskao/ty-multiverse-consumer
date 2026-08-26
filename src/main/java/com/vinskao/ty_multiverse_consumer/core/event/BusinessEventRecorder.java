package com.vinskao.ty_multiverse_consumer.core.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tw.com.ty.common.event.BusinessEvent;
import tw.com.ty.common.event.BusinessEventType;

/**
 * 業務事件記錄器
 *
 * <p>把 {@link BusinessEvent} 序列化後寫進 business_event_outbox。
 * 呼叫端必須把它包在業務寫入的同一個 transaction 內
 * （見 {@code TransactionalOperator} 的使用方式），
 * 這樣「業務資料改了但事件沒留下」或反之的情況才不會發生。</p>
 *
 * @author TY Backend Team
 * @since 2026-08
 */
@Service
@EnableConfigurationProperties(OutboxProperties.class)
public class BusinessEventRecorder {

    private static final Logger logger = LoggerFactory.getLogger(BusinessEventRecorder.class);

    /** 事件來源標識，會寫進 BusinessEvent.source */
    public static final String SOURCE = "ty-multiverse-consumer";

    private final BusinessEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final OutboxProperties properties;

    public BusinessEventRecorder(BusinessEventOutboxRepository outboxRepository,
                                 ObjectMapper objectMapper,
                                 OutboxProperties properties) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 記錄一筆成功事件
     *
     * @param operation   操作前綴，例如 {@link BusinessEventType#PEOPLE_UPDATE}
     * @param aggregateId 受影響的資料識別，批次操作可傳 null
     * @param requestId   來自 AsyncMessageDTO 的 requestId
     * @param payload     事件內容（不得含機密；更新事件建議只放 before/after 差異）
     */
    public Mono<Void> recordSucceeded(String operation,
                                      String aggregateType,
                                      String aggregateId,
                                      String requestId,
                                      java.util.Map<String, Object> payload) {
        return record(BusinessEvent.builder()
                .eventType(BusinessEventType.succeeded(operation))
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .requestId(requestId)
                .source(SOURCE)
                .payload(payload)
                .build());
    }

    /**
     * 記錄一筆失敗事件。只保留錯誤代碼與訊息首行摘要，不含 stack trace。
     */
    public Mono<Void> recordFailed(String operation,
                                   String aggregateType,
                                   String aggregateId,
                                   String requestId,
                                   Throwable error) {
        BusinessEvent.ErrorInfo info = BusinessEvent.ErrorInfo.of(
                error == null ? "UNKNOWN" : error.getClass().getSimpleName(),
                error == null ? null : error.getMessage());
        return record(BusinessEvent.builder()
                .eventType(BusinessEventType.failed(operation))
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .requestId(requestId)
                .source(SOURCE)
                .error(info)
                .build());
    }

    /**
     * 記錄任意一筆事件
     *
     * <p><b>錯誤一律往外傳。</b>序列化失敗或寫入失敗時若吞掉錯誤，業務 transaction 會照常
     * commit 但 outbox 沒有紀錄，正好違反 transactional outbox 的核心保證。因此這裡必須
     * 讓錯誤傳出去，由 {@link BusinessEventTransaction} 的 transaction 一併回滾。</p>
     *
     * <p>唯一可以 quiet failure 的是 failed 事件的補記流程——業務 transaction 此時已經
     * 回滾，稽核寫入再失敗也不應該蓋掉原本的業務錯誤。那個吞例外的行為放在
     * {@link BusinessEventTransaction#recordFailedQuietly} 裡，不在這一層。</p>
     */
    public Mono<Void> record(BusinessEvent event) {
        if (!properties.isEnabled()) {
            return Mono.empty();
        }
        return serialize(event).flatMap(payloadJson -> outboxRepository.append(
                        event.getEventId(),
                        event.getEventType(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        event.getRequestId(),
                        payloadJson,
                        event.getOccurredAt())
                .doOnNext(rows -> {
                    if (rows == 0) {
                        logger.debug("outbox 已存在相同 eventId，略過: {}", event.getEventId());
                    } else {
                        logger.debug("📝 已寫入 outbox: eventType={}, eventId={}, requestId={}",
                                event.getEventType(), event.getEventId(), event.getRequestId());
                    }
                })
                .doOnError(e -> logger.error("❌ 寫入 outbox 失敗，業務 transaction 將一併回滾: "
                                + "eventType={}, requestId={}, cause={}",
                        event.getEventType(), event.getRequestId(), e.getMessage()))
                .then());
    }

    /**
     * 序列化事件；失敗時回傳 {@code Mono.error}，讓呼叫端的 transaction 回滾。
     */
    private Mono<String> serialize(BusinessEvent event) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
                .onErrorMap(e -> {
                    logger.error("❌ BusinessEvent 序列化失敗，業務 transaction 將一併回滾: "
                                    + "eventType={}, requestId={}, cause={}",
                            event.getEventType(), event.getRequestId(), e.getMessage());
                    return new BusinessEventSerializationException(event.getEventType(), e);
                });
    }

    /**
     * 事件序列化失敗。刻意是 unchecked exception，讓它能穿過 reactive chain
     * 觸發 transaction 回滾。
     */
    public static class BusinessEventSerializationException extends RuntimeException {
        public BusinessEventSerializationException(String eventType, Throwable cause) {
            super("無法序列化 BusinessEvent: eventType=" + eventType, cause);
        }
    }
}
