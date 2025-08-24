package com.vinskao.ty_multiverse_consumer.core.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.vinskao.ty_multiverse_consumer.service.DatabaseConnectionService;

/**
 * 通用 Consumer
 * 
 * 處理通用的消息隊列，如傷害計算和獲取所有角色
 * 只在 RabbitMQ 啟用時生效
 * 
 * @author TY Backend Team
 * @version 1.0
 * @since 2024
 */
@Component
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true")
public class CommonConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(CommonConsumer.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private com.vinskao.ty_multiverse_consumer.module.people.service.PeopleService peopleService;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private DatabaseConnectionService databaseConnectionService;
    
    /**
     * 處理傷害計算請求
     */
    @RabbitListener(queues = "damage-calculation")
    public void handleDamageCalculation(String messageJson) {
        try {
            logger.info("收到傷害計算請求: {}", messageJson);
            
            // 解析消息
            DamageCalculationMessageDTO message = objectMapper.readValue(messageJson, DamageCalculationMessageDTO.class);
            String characterName = message.getCharacterName();
            
            // 執行傷害計算邏輯
            logger.info("開始計算角色傷害: characterName={}, requestId={}", 
                       characterName, message.getRequestId());
            
            // TODO: 調用傷害計算服務
            // damageCalculationService.calculateDamage(characterName);
            
            logger.info("傷害計算完成: characterName={}, requestId={}", 
                       characterName, message.getRequestId());
            
        } catch (Exception e) {
            logger.error("處理傷害計算請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 處理獲取所有角色請求
     */
    @RabbitListener(queues = "people-get-all")
    public void handleGetAllPeople(String messageJson) {
        try {
            logger.info("收到獲取所有角色請求: {}", messageJson);
            
            // 解析消息
            PeopleGetAllMessageDTO message = objectMapper.readValue(messageJson, PeopleGetAllMessageDTO.class);
            
            logger.info("開始獲取所有角色: requestId={}", message.getRequestId());
            
            // 等待數據庫連接可用
            logger.info("等待數據庫連接可用...");
            if (!databaseConnectionService.isConnectionAvailable()) {
                logger.info("數據庫連接不可用，開始等待...");
                databaseConnectionService.forceWaitForConnection();
            }
            
            // 實際調用 People 服務獲取所有角色
            var peopleList = peopleService.getAllPeopleOptimized();
            
            logger.info("成功獲取所有角色: count={}, requestId={}", 
                       peopleList.size(), message.getRequestId());
            
            // 打印詳細的數據庫數據
            if (peopleList.isEmpty()) {
                logger.info("數據庫中目前沒有角色數據");
            } else {
                logger.info("數據庫中的角色數據:");
                peopleList.forEach(people -> {
                    logger.info("  - 角色: name={}, age={}, gender={}, job={}, attributes={}", 
                               people.getName(), people.getAge(), people.getGender(), 
                               people.getJob(), people.getAttributes());
                });
            }
            
            // 構建回傳消息
            PeopleGetAllResponseDTO response = new PeopleGetAllResponseDTO();
            response.setRequestId(message.getRequestId());
            response.setStatus("success");
            response.setMessage("成功獲取所有角色數據");
            response.setData(peopleList);
            response.setTimestamp(System.currentTimeMillis());
            response.setErrorCode(null);
            response.setErrorDetails(null);
            
            // 發送回傳消息到 producer
            String responseJson = objectMapper.writeValueAsString(response);
            rabbitTemplate.convertAndSend("people-response", "people.get-all.response", responseJson);
            
            logger.info("已發送回傳消息: requestId={}, dataSize={}", message.getRequestId(), peopleList.size());
            
        } catch (Exception e) {
            logger.error("處理獲取所有角色請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 傷害計算消息 DTO
     */
    public static class DamageCalculationMessageDTO {
        private String requestId;
        private String characterName;
        
        // Getters and Setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getCharacterName() { return characterName; }
        public void setCharacterName(String characterName) { this.characterName = characterName; }
    }
    
    /**
     * 獲取所有角色消息 DTO
     */
    public static class PeopleGetAllMessageDTO {
        private String requestId;
        private String operation;
        
        // Getters and Setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
    }
    
             /**
          * 獲取所有角色回傳消息 DTO
          * 與 Producer 端的 ProducerResponseDTO 保持一致
          */
         public static class PeopleGetAllResponseDTO {
             private String requestId;
             private String status;
             private String message;
             private Object data;
             private Long timestamp;
             private String errorCode;
             private String errorDetails;
             
             // Getters and Setters
             public String getRequestId() { return requestId; }
             public void setRequestId(String requestId) { this.requestId = requestId; }
             
             public String getStatus() { return status; }
             public void setStatus(String status) { this.status = status; }
             
             public String getMessage() { return message; }
             public void setMessage(String message) { this.message = message; }
             
             public Object getData() { return data; }
             public void setData(Object data) { this.data = data; }
             
             public Long getTimestamp() { return timestamp; }
             public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
             
             public String getErrorCode() { return errorCode; }
             public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
             
             public String getErrorDetails() { return errorDetails; }
             public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
         }
}
