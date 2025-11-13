package com.vinskao.ty_multiverse_consumer.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinskao.ty_multiverse_consumer.core.dto.AsyncResultMessage;
import com.vinskao.ty_multiverse_consumer.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;

/**
 * 異步結果服務
 * 
 * 負責將 Consumer 的處理結果發送到 async-result 隊列
 * 只在 RabbitMQ 啟用時生效
 * 
 * @author TY Backend Team
 * @version 1.0
 * @since 2024
 */
@Service
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true")
public class AsyncResultService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncResultService.class);
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private Sender reactiveSender;
    
    /**
     * 發送成功結果
     */
    public void sendCompletedResult(String requestId, Object data) {
        logger.info("🔄 準備發送成功結果: requestId={}, dataType={}",
                   requestId, data != null ? data.getClass().getSimpleName() : "null");
        AsyncResultMessage resultMessage = AsyncResultMessage.completed(requestId, data);
        sendAsyncResult(resultMessage);
    }
    
    /**
     * 發送失敗結果
     */
    public void sendFailedResult(String requestId, String error) {
        logger.warn("🔄 準備發送失敗結果: requestId={}, error={}", requestId, error);
        AsyncResultMessage resultMessage = AsyncResultMessage.failed(requestId, error);
        sendAsyncResult(resultMessage);
    }
    
    /**
     * 發送成功結果 - Reactive 版本
     */
    public Mono<Void> sendCompletedResultReactive(String requestId, Object data) {
        logger.info("🔄 準備發送成功結果 (Reactive): requestId={}, dataType={}",
                   requestId, data != null ? data.getClass().getSimpleName() : "null");
        AsyncResultMessage resultMessage = AsyncResultMessage.completed(requestId, data);
        return sendAsyncResultReactive(resultMessage);
    }
    
    /**
     * 發送失敗結果 - Reactive 版本
     */
    public Mono<Void> sendFailedResultReactive(String requestId, String error) {
        logger.warn("🔄 準備發送失敗結果 (Reactive): requestId={}, error={}", requestId, error);
        AsyncResultMessage resultMessage = AsyncResultMessage.failed(requestId, error);
        return sendAsyncResultReactive(resultMessage);
    }
    
    /**
     * 發送異步結果消息
     */
    private void sendAsyncResult(AsyncResultMessage resultMessage) {
        try {
            String messageJson = objectMapper.writeValueAsString(resultMessage);

            logger.info("📤 準備發送異步結果消息到 RabbitMQ:");
            logger.info("  - 交換機: {}", RabbitMQConfig.MAIN_EXCHANGE);
            logger.info("  - 路由鍵: {}", RabbitMQConfig.ASYNC_RESULT_ROUTING_KEY);
            logger.info("  - 隊列: {}", RabbitMQConfig.ASYNC_RESULT_QUEUE);
            logger.info("  - 消息內容: {}", messageJson);

            // 按照 Producer 規範，使用硬編碼的交換機和路由鍵
            logger.info("🔧 使用 Producer 規範的發送方式:");
            logger.info("  - 交換機: tymb-exchange");
            logger.info("  - 路由鍵: async.result");

            try {
                // 直接發送對象，讓 RabbitTemplate 的 MessageConverter 自動序列化
                // 避免雙重序列化問題（messageJson 已經是字符串，再次序列化會導致雙重編碼）
                rabbitTemplate.convertAndSend(
                    "tymb-exchange",    // 硬編碼以確保與 Producer 一致
                    "async.result",      // 硬編碼以確保與 Producer 一致
                    resultMessage       // 直接發送對象，而非手動序列化的字符串
                );

                logger.info("✅ 成功發送異步結果消息: requestId={}, status={}, source={}",
                           resultMessage.getRequestId(), resultMessage.getStatus(), resultMessage.getSource());
            } catch (Exception sendException) {
                logger.error("❌ RabbitMQ 發送失敗: {}", sendException.getMessage(), sendException);
                throw sendException;
            }

        } catch (JsonProcessingException e) {
            logger.error("❌ 序列化異步結果消息失敗: {}", e.getMessage(), e);
            throw new RuntimeException("消息序列化失敗", e);
        } catch (Exception e) {
            logger.error("❌ 發送異步結果消息失敗: {}", e.getMessage(), e);
            logger.error("  - 交換機: {}", RabbitMQConfig.MAIN_EXCHANGE);
            logger.error("  - 路由鍵: {}", RabbitMQConfig.ASYNC_RESULT_ROUTING_KEY);
            logger.error("  - requestId: {}", resultMessage.getRequestId());
            throw new RuntimeException("消息發送失敗", e);
        }
    }
    
    /**
     * 發送異步結果消息 - Reactive 版本
     */
    private Mono<Void> sendAsyncResultReactive(AsyncResultMessage resultMessage) {
        if (reactiveSender == null) {
            logger.warn("⚠️ Reactive Sender 不可用，使用 blocking 版本");
            return Mono.fromRunnable(() -> sendAsyncResult(resultMessage));
        }
        
        return Mono.fromCallable(() -> {
                try {
                    return objectMapper.writeValueAsString(resultMessage);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("消息序列化失敗", e);
                }
            })
            .flatMap(messageJson -> {
                logger.info("📤 準備發送異步結果消息 (Reactive):");
                logger.info("  - 交換機: tymb-exchange");
                logger.info("  - 路由鍵: async.result");
                logger.info("  - 消息內容: {}", messageJson);
                
                OutboundMessage outboundMessage = new OutboundMessage(
                    "tymb-exchange",
                    "async.result", 
                    messageJson.getBytes()
                );
                
                return reactiveSender.send(Mono.just(outboundMessage))
                    .doOnSuccess(v -> logger.info("✅ 成功發送異步結果消息 (Reactive): requestId={}, status={}",
                                                 resultMessage.getRequestId(), resultMessage.getStatus()))
                    .doOnError(error -> logger.error("❌ Reactive 發送失敗: {}", error.getMessage(), error));
            })
            .onErrorResume(error -> {
                logger.error("❌ Reactive 發送異步結果消息失敗: {}", error.getMessage(), error);
                return Mono.error(new RuntimeException("Reactive 消息發送失敗", error));
            });
    }
}
