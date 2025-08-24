package com.vinskao.ty_multiverse_consumer.core.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true")
public class ResponseConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(ResponseConsumer.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 監聽回傳消息
     */
    @RabbitListener(queues = "people.response.queue")
    public void handlePeopleResponse(String messageJson) {
        try {
            logger.info("收到回傳消息: {}", messageJson);
            
            // 解析回傳消息
            CommonConsumer.PeopleGetAllResponseDTO response = 
                objectMapper.readValue(messageJson, CommonConsumer.PeopleGetAllResponseDTO.class);
            
            logger.info("回傳消息解析成功: requestId={}, status={}, dataType={}", 
                       response.getRequestId(), response.getStatus(), 
                       response.getData() != null ? response.getData().getClass().getSimpleName() : "null");
            
            // 這裡可以添加處理回傳消息的邏輯
            // 例如：存儲到緩存、發送通知等
            
        } catch (Exception e) {
            logger.error("處理回傳消息失敗: {}", e.getMessage(), e);
        }
    }
}
