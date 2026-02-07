package com.vinskao.ty_multiverse_consumer.core.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinskao.ty_multiverse_consumer.config.RabbitMQConfig;
import com.vinskao.ty_multiverse_consumer.core.dto.AsyncResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.Receiver;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.util.retry.Retry;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 完全 Reactive AsyncResult Consumer
 * 
 * 使用 Reactor RabbitMQ 監聽 async-result 隊列
 * 處理異步結果消息的日誌記錄和監控
 * 
 * @author TY Backend Team
 * @version 2.0
 * @since 2024
 */
@Component
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true")
@ConditionalOnProperty(name = "ty.multiverse.consumer.async-result-monitor.enabled",
    havingValue = "true", matchIfMissing = false)
public class ReactiveAsyncResultConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveAsyncResultConsumer.class);

    @Autowired
    private Receiver reactiveReceiverHighPriority; // 使用高優先級接收器

    @Autowired
    private ObjectMapper objectMapper;

    private final Disposable.Composite subscriptions = Disposables.composite();

    /**
     * 啟動 reactive 消費者
     */
    @PostConstruct
    public void startConsumer() {
        logger.info("🚀 啟動 Reactive AsyncResult Consumer...");
        
        startAsyncResultConsumer();
        
        logger.info("✅ Reactive AsyncResult Consumer 啟動完成");
    }

    /**
     * AsyncResult 消費者 - 監控和日誌記錄
     * 
     * 使用高優先級接收器，prefetch=1，確保快速處理
     */
    private void startAsyncResultConsumer() {
        subscriptions.add(
            reactiveReceiverHighPriority
                .consumeManualAck(RabbitMQConfig.ASYNC_RESULT_QUEUE, new ConsumeOptions().qos(1))
                .flatMap(this::handleAsyncResult, 1) // 序列化處理，避免日誌混亂
                .doOnError(error -> logger.error("❌ AsyncResult 消費者發生錯誤: {}", error.getMessage()))
                .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(5))
                    .maxBackoff(java.time.Duration.ofSeconds(30))) // 有限重試，避免無限循環
                .subscribe()
        );
        
        logger.info("📡 啟動 AsyncResult Reactive Consumer (concurrency=1, prefetch=1)");
    }

    /**
     * 處理異步結果消息 - 完全 reactive
     */
    private Mono<Void> handleAsyncResult(AcknowledgableDelivery delivery) {
        String messageJson = new String(delivery.getBody());
        
        return Mono.fromCallable(() -> {
                logger.info("🔄 收到異步結果消息: {}", messageJson);
                return objectMapper.readValue(messageJson, AsyncResultMessage.class);
            })
            .flatMap(resultMessage -> {
                logger.info("📨 解析成功 - requestId={}, status={}, source={}",
                           resultMessage.getRequestId(), resultMessage.getStatus(), resultMessage.getSource());
                
                return processAsyncResult(resultMessage)
                    .doOnSuccess(v -> {
                        logger.info("🎉 AsyncResult 處理完成: requestId={}", resultMessage.getRequestId());
                        delivery.ack(); // 手動 ACK
                    })
                    .doOnError(error -> {
                        logger.error("❌ AsyncResult 處理失敗: requestId={}, error={}", 
                                   resultMessage.getRequestId(), error.getMessage());
                        delivery.nack(false); // 處理失敗，不重新入隊（避免無限循環）
                    });
            })
            .onErrorResume(parseError -> {
                logger.error("❌ 無法解析異步結果消息: {}, error={}", messageJson, parseError.getMessage());
                delivery.nack(false); // 解析錯誤，不重新入隊
                return Mono.empty();
            })
            .then();
    }

    /**
     * 處理異步結果的業務邏輯
     */
    private Mono<Void> processAsyncResult(AsyncResultMessage resultMessage) {
        return Mono.fromRunnable(() -> {
            // 根據狀態處理結果
            switch (resultMessage.getStatus()) {
                case "completed":
                    handleCompletedResult(resultMessage);
                    break;

                case "failed":
                    handleFailedResult(resultMessage);
                    break;

                default:
                    handleUnknownResult(resultMessage);
            }
        });
    }

    /**
     * 處理成功結果
     */
    private void handleCompletedResult(AsyncResultMessage resultMessage) {
        logger.info("✅ 異步處理完成: requestId={}, dataSize={}",
                   resultMessage.getRequestId(),
                   resultMessage.getData() != null ? "有數據" : "無數據");
        
        if (resultMessage.getData() != null) {
            logger.info("📊 處理結果數據類型: {}", resultMessage.getData().getClass().getSimpleName());
            
            // 如果是列表類型，記錄數量
            if (resultMessage.getData() instanceof java.util.List<?> list) {
                logger.info("📈 結果數據數量: {} 項", list.size());
            }
        }
        
        // 這裡可以添加成功結果的後續處理邏輯
        // 例如：更新緩存、發送通知、記錄統計等
        logSuccessMetrics(resultMessage);
    }

    /**
     * 處理失敗結果
     */
    private void handleFailedResult(AsyncResultMessage resultMessage) {
        logger.error("❌ 異步處理失敗: requestId={}, error={}",
                    resultMessage.getRequestId(), resultMessage.getError());
        
        // 這裡可以添加失敗結果的後續處理邏輯
        // 例如：記錄錯誤統計、發送警報、重試機制等
        logFailureMetrics(resultMessage);
    }

    /**
     * 處理未知狀態結果
     */
    private void handleUnknownResult(AsyncResultMessage resultMessage) {
        logger.warn("⚠️ 未知的異步結果狀態: requestId={}, status={}",
                   resultMessage.getRequestId(), resultMessage.getStatus());
        
        // 記錄未知狀態統計
        logUnknownStatusMetrics(resultMessage);
    }

    /**
     * 記錄成功指標
     */
    private void logSuccessMetrics(AsyncResultMessage resultMessage) {
        // 這裡可以集成 Micrometer 或其他監控工具
        logger.debug("📊 記錄成功指標: requestId={}, timestamp={}", 
                    resultMessage.getRequestId(), resultMessage.getTimestamp());
    }

    /**
     * 記錄失敗指標
     */
    private void logFailureMetrics(AsyncResultMessage resultMessage) {
        // 這裡可以集成 Micrometer 或其他監控工具
        logger.debug("📊 記錄失敗指標: requestId={}, error={}", 
                    resultMessage.getRequestId(), resultMessage.getError());
    }

    /**
     * 記錄未知狀態指標
     */
    private void logUnknownStatusMetrics(AsyncResultMessage resultMessage) {
        // 這裡可以集成 Micrometer 或其他監控工具
        logger.debug("📊 記錄未知狀態指標: requestId={}, status={}", 
                    resultMessage.getRequestId(), resultMessage.getStatus());
    }

    @PreDestroy
    public void shutdown() {
        logger.info("🛑 關閉 Reactive AsyncResult Consumer...");
        subscriptions.dispose();
    }
}
