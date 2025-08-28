package com.vinskao.ty_multiverse_consumer.core.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.vinskao.ty_multiverse_consumer.core.dto.AsyncMessageDTO;
import com.vinskao.ty_multiverse_consumer.core.service.AsyncResultService;
import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.People;
import com.vinskao.ty_multiverse_consumer.module.people.service.PeopleService;

import java.util.List;

/**
 * People 請求處理 Consumer
 * 
 * 負責處理 People 相關的 RabbitMQ 請求
 * 只在 RabbitMQ 啟用時生效
 * 
 * @author TY Backend Team
 * @version 1.0
 * @since 2024
 */
@Component
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true")
public class PeopleConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(PeopleConsumer.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private PeopleService peopleService;
    
    @Autowired
    private AsyncResultService asyncResultService;
    
    /**
     * 監聽 People 獲取所有請求
     */
    @RabbitListener(queues = "people-get-all", concurrency = "2")
    public void handleGetAllPeople(String messageJson) {
        try {
            logger.info("收到獲取所有角色請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            
            logger.info("開始獲取所有角色: requestId={}", requestId);
            
            // 處理請求
            List<People> peopleList = peopleService.getAllPeopleOptimized();
            
            // 記錄處理結果
            logger.info("成功獲取所有角色: count={}, requestId={}", peopleList.size(), requestId);
            logger.info("數據庫中的角色數據: ");
            for (People people : peopleList) {
                logger.info("  - 角色: name={}, age={}, gender={}, job={}, attributes={}", 
                           people.getName(), people.getAge(), people.getGender(), 
                           people.getJob(), people.getAttributes());
            }
            
            // 發送成功結果給 Producer
            asyncResultService.sendCompletedResult(requestId, peopleList);
            
            logger.info("回傳消息解析成功: requestId={}, status=success, count={}", 
                       requestId, peopleList.size());
            
        } catch (Exception e) {
            logger.error("處理獲取所有角色請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤結果給 Producer
                asyncResultService.sendFailedResult(requestId, "獲取角色列表失敗: " + e.getMessage());
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
    
    /**
     * 監聽 People 根據名稱獲取請求
     */
    @RabbitListener(queues = "people-get-by-name", concurrency = "2")
    public void handleGetPeopleByName(String messageJson) {
        try {
            logger.info("收到根據名稱獲取角色請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            String name = (String) message.getPayload();
            
            logger.info("開始根據名稱獲取角色: name={}, requestId={}", name, requestId);
            
            // 處理請求
            var peopleOptional = peopleService.getPeopleByName(name);
            
            if (peopleOptional.isPresent()) {
                People people = peopleOptional.get();
                logger.info("成功獲取角色: name={}, requestId={}", name, requestId);
                
                // 發送成功結果給 Producer
                asyncResultService.sendCompletedResult(requestId, people);
            } else {
                logger.warn("角色不存在: name={}, requestId={}", name, requestId);
                
                // 發送錯誤結果給 Producer
                asyncResultService.sendFailedResult(requestId, "角色不存在: " + name);
            }
            
        } catch (Exception e) {
            logger.error("處理根據名稱獲取角色請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤結果給 Producer
                asyncResultService.sendFailedResult(requestId, "獲取角色失敗: " + e.getMessage());
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
    
    /**
     * 監聽 People 刪除所有請求
     */
    @RabbitListener(queues = "people-delete-all", concurrency = "2")
    public void handleDeleteAllPeople(String messageJson) {
        try {
            logger.info("收到刪除所有角色請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            
            logger.info("開始刪除所有角色: requestId={}", requestId);
            
            // 處理請求
            peopleService.deleteAllPeople();
            
            logger.info("成功刪除所有角色: requestId={}", requestId);
            
            // 發送成功結果給 Producer
            asyncResultService.sendCompletedResult(requestId, null);
            
        } catch (Exception e) {
            logger.error("處理刪除所有角色請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤結果給 Producer
                asyncResultService.sendFailedResult(requestId, "刪除所有角色失敗: " + e.getMessage());
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
}
