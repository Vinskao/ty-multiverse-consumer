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
     * 監聽 People 獲取所有請求 - 完全符合 Producer 規範
     *
     * 處理來自 Producer 的 POST /tymb/people/get-all 請求
     * 響應格式完全符合規範要求
     */
    @RabbitListener(queues = "people-get-all", concurrency = "2")
    public void handleGetAllPeople(String messageJson) {
        try {
            logger.info("🎯 收到 Producer 的 People Get-All 請求: {}", messageJson);

            // 解析請求消息
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();

            logger.info("📝 解析請求: requestId={}, endpoint={}, method={}",
                       requestId, message.getEndpoint(), message.getMethod());
            logger.info("⏰ 請求時間戳: {}", message.getTimestamp());
            logger.info("🏷️  來源標識: {}", message.getSource());

            // 處理請求 - 獲取數據庫中的所有 People 數據
            logger.info("🔄 開始查詢數據庫所有角色數據...");
            List<People> peopleList = peopleService.getAllPeopleOptimized();

            // 記錄查詢結果統計
            logger.info("✅ 數據庫查詢完成: 共獲取 {} 個角色", peopleList.size());

            // 詳細記錄前幾個角色的信息（用於調試）
            if (!peopleList.isEmpty()) {
                logger.info("📊 角色數據樣本:");
                int sampleSize = Math.min(5, peopleList.size());
                for (int i = 0; i < sampleSize; i++) {
                    People people = peopleList.get(i);
                    logger.info("  - 角色[{}]: name={}, codeName={}, gender={}, job={}, age={}",
                               i + 1, people.getName(), people.getCodeName(),
                               people.getGender(), people.getJob(), people.getAge());
                }
                if (peopleList.size() > sampleSize) {
                    logger.info("  ... 還有 {} 個角色", peopleList.size() - sampleSize);
                }
            }

            // 發送成功結果給 Producer - 使用規範格式
            logger.info("📤 準備發送響應消息到 async-result 隊列");
            asyncResultService.sendCompletedResult(requestId, peopleList);

            logger.info("🎉 People Get-All 請求處理完成!");
            logger.info("   - requestId: {}", requestId);
            logger.info("   - 狀態: completed");
            logger.info("   - 數據量: {} 個角色", peopleList.size());
            logger.info("   - 發送至: tymb-exchange -> async.result");

        } catch (Exception e) {
            logger.error("❌ People Get-All 請求處理失敗: {}", e.getMessage(), e);

            // 嘗試解析請求ID並發送錯誤響應
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();

                logger.warn("🔄 發送錯誤響應: requestId={}", requestId);
                asyncResultService.sendFailedResult(requestId, "獲取角色列表失敗: " + e.getMessage());

            } catch (Exception parseError) {
                logger.error("❌ 無法解析請求ID，無法發送錯誤響應: {}", parseError.getMessage());
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
