package com.vinskao.ty_multiverse_consumer.core.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinskao.ty_multiverse_consumer.core.dto.AsyncResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 異步結果 Consumer
 * 
 * 監聽 async-result 隊列，處理異步結果消息
 * 只在 RabbitMQ 啟用時生效
 * 
 * @author TY Backend Team
 * @version 1.0
 * @since 2024
 */
@Component
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true")
public class AsyncResultConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncResultConsumer.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 監聽異步結果消息
     */
    @RabbitListener(queues = "async-result", concurrency = "2")
    public void handleAsyncResult(String messageJson) {
        try {
            logger.info("🔄 收到異步結果消息: {}", messageJson);

            // 解析 AsyncResultMessage
            AsyncResultMessage resultMessage = objectMapper.readValue(messageJson, AsyncResultMessage.class);

            logger.info("📨 解析成功 - requestId={}, status={}, source={}",
                       resultMessage.getRequestId(), resultMessage.getStatus(), resultMessage.getSource());

            // 根據狀態處理結果
            switch (resultMessage.getStatus()) {
                case "completed":
                    logger.info("✅ 異步處理完成: requestId={}, dataSize={}",
                               resultMessage.getRequestId(),
                               resultMessage.getData() != null ? "有數據" : "無數據");
                    if (resultMessage.getData() != null) {
                        logger.info("📊 處理結果數據類型: {}", resultMessage.getData().getClass().getSimpleName());
                    }
                    break;

                case "failed":
                    logger.error("❌ 異步處理失敗: requestId={}, error={}",
                                resultMessage.getRequestId(), resultMessage.getError());
                    break;

                default:
                    logger.warn("⚠️  未知的異步結果狀態: requestId={}, status={}",
                               resultMessage.getRequestId(), resultMessage.getStatus());
            }

        } catch (Exception e) {
            logger.error("💥 處理異步結果消息失敗: {}", e.getMessage(), e);
            logger.error("原始消息內容: {}", messageJson);
        }
    }
}
