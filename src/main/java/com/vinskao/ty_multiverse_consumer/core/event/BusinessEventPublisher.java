package com.vinskao.ty_multiverse_consumer.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tw.com.ty.common.event.BusinessEventType;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Outbox 發布器
 *
 * <p>定期把 business_event_outbox 中尚未發布的事件送到 tymb-event-exchange，
 * 收到 RabbitMQ 的 publisher confirm ack 之後才回填 published_at。</p>
 *
 * <h3>保證</h3>
 * <ul>
 *   <li><b>不丟事件</b>：RabbitMQ 中斷時事件留在 outbox，恢復後自動補送。</li>
 *   <li><b>至少一次</b>：ack 之後、markPublished 之前若當機，同一事件會再送一次；
 *       下游必須以 eventId 去重（AMQP messageId 也帶了同一個值）。</li>
 *   <li><b>退避</b>：連續失敗時 next_attempt_at 指數往後推，壞事件不會塞住整個輪詢。</li>
 * </ul>
 *
 * @author TY Backend Team
 * @since 2026-08
 */
@Component
@ConditionalOnProperty(name = "tymb.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class BusinessEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(BusinessEventPublisher.class);

    private final BusinessEventOutboxRepository outboxRepository;
    private final RabbitTemplate eventRabbitTemplate;
    private final OutboxProperties properties;

    public BusinessEventPublisher(BusinessEventOutboxRepository outboxRepository,
                                  @Qualifier("eventRabbitTemplate") RabbitTemplate eventRabbitTemplate,
                                  OutboxProperties properties) {
        this.outboxRepository = outboxRepository;
        this.eventRabbitTemplate = eventRabbitTemplate;
        this.properties = properties;
    }

    /**
     * 輪詢 outbox 並發布。第一版刻意用 Spring 排程輪詢，資料量變大再考慮 Debezium CDC。
     */
    @Scheduled(fixedDelayString = "${tymb.outbox.poll-interval-ms:2000}")
    public void publishPending() {
        AtomicInteger published = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        try {
            outboxRepository.findDue(properties.getBatchSize())
                    .toStream()
                    .forEach(pending -> {
                        if (publishOne(pending)) {
                            published.incrementAndGet();
                        } else {
                            failed.incrementAndGet();
                        }
                    });
        } catch (Exception e) {
            // 讀 outbox 失敗（例如 DB 短暫不可用）不應讓排程執行緒死掉
            logger.warn("⚠️ 讀取 outbox 失敗，下一輪重試: {}", e.getMessage());
            return;
        }
        if (published.get() > 0 || failed.get() > 0) {
            logger.info("📤 outbox 發布完成: 成功={}, 失敗={}", published.get(), failed.get());
        }
    }

    /**
     * 發布單一事件並等待 confirm。
     *
     * @return true 表示已 ack 並標記為已發布
     */
    private boolean publishOne(BusinessEventOutboxRepository.PendingEvent pending) {
        String routingKey = BusinessEventType.routingKey(pending.eventType());
        try {
            Message message = MessageBuilder
                    .withBody(pending.payloadJson().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    // messageId = eventId，下游直接拿它做冪等去重
                    .setMessageId(pending.eventId())
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();

            CorrelationData correlation = new CorrelationData(pending.eventId());
            eventRabbitTemplate.send(EventRabbitMQConfig.TYMB_EVENT_EXCHANGE, routingKey, message, correlation);

            CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(properties.getConfirmTimeoutMs(), TimeUnit.MILLISECONDS);

            if (confirm == null || !confirm.isAck()) {
                return markFailed(pending, "nack: " + (confirm == null ? "no confirm" : confirm.getReason()));
            }
            if (correlation.getReturned() != null) {
                // broker ack 了但沒有任何 queue 收到（binding 不存在），視為失敗以免默默丟掉稽核事件
                return markFailed(pending, "returned: message was unroutable with routing key " + routingKey);
            }

            outboxRepository.markPublished(pending.id()).block();
            return true;
        } catch (Exception e) {
            return markFailed(pending, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private boolean markFailed(BusinessEventOutboxRepository.PendingEvent pending, String reason) {
        int attempts = (pending.attemptCount() == null ? 0 : pending.attemptCount()) + 1;
        long backoffSeconds = backoffSeconds(attempts);
        if (attempts >= properties.getAlertAfterAttempts()) {
            logger.error("🚨 outbox 事件連續失敗 {} 次，仍保留待重試: eventId={}, eventType={}, reason={}",
                    attempts, pending.eventId(), pending.eventType(), reason);
        } else {
            logger.warn("⚠️ outbox 事件發布失敗（第 {} 次），{} 秒後重試: eventId={}, reason={}",
                    attempts, backoffSeconds, pending.eventId(), reason);
        }
        try {
            outboxRepository.markFailed(pending.id(), reason, backoffSeconds).block();
        } catch (Exception e) {
            logger.error("❌ 無法更新 outbox 失敗狀態: eventId={}, cause={}", pending.eventId(), e.getMessage());
        }
        return false;
    }

    /**
     * 指數退避：base * 2^(attempts-1)，上限 maxBackoffMs
     */
    private long backoffSeconds(int attempts) {
        int shift = Math.min(attempts - 1, 20);
        long backoffMs = Math.min(properties.getBackoffBaseMs() << shift, properties.getMaxBackoffMs());
        return Math.max(1, backoffMs / 1000);
    }
}
