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
import com.vinskao.ty_multiverse_consumer.module.weapon.domain.vo.Weapon;
import com.vinskao.ty_multiverse_consumer.module.weapon.service.WeaponService;

import java.util.List;
import java.util.Optional;

/**
 * Weapon 請求處理 Consumer
 * 
 * 負責處理 Weapon 相關的 RabbitMQ 請求
 * 使用新的 AsyncResultService 發送結果到 async-result 隊列
 * 只在 RabbitMQ 啟用時生效
 * 
 * @author TY Backend Team
 * @version 1.0
 * @since 2024
 */
@Component
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true")
@ConditionalOnProperty(name = "spring.rabbitmq.legacy.enabled", havingValue = "true", matchIfMissing = false)
public class WeaponConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(WeaponConsumer.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private WeaponService weaponService;
    
    @Autowired
    private AsyncResultService asyncResultService;
    
    /**
     * 監聽 Weapon 獲取所有請求
     */
    @RabbitListener(queues = "weapon-get-all", concurrency = "2")
    public void handleGetAllWeapons(String messageJson) {
        try {
            logger.info("收到獲取所有武器請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            
            logger.info("開始獲取所有武器: requestId={}", requestId);
            
            // 處理請求
            List<Weapon> weapons = weaponService.getAllWeapons().collectList().block();
            
            logger.info("成功獲取所有武器: count={}, requestId={}", weapons.size(), requestId);
            
            // 發送成功結果給 Producer
            asyncResultService.sendCompletedResult(
                requestId,
                weapons
            );
            
        } catch (Exception e) {
            logger.error("處理獲取所有武器請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤結果給 Producer
                asyncResultService.sendFailedResult(
                    requestId,
                    "獲取武器列表失敗: " + e.getMessage()
                );
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
    
    /**
     * 監聽 Weapon 根據名稱獲取請求
     */
    @RabbitListener(queues = "weapon-get-by-name", concurrency = "2")
    public void handleGetWeaponByName(String messageJson) {
        try {
            logger.info("收到根據名稱獲取武器請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            String name = (String) message.getPayload();
            
            logger.info("開始根據名稱獲取武器: name={}, requestId={}", name, requestId);
            
            // 處理請求
            Weapon weapon = weaponService.getWeaponById(name).block();

            if (weapon != null) {
                logger.info("成功獲取武器: name={}, requestId={}", name, requestId);

                // 發送成功結果給 Producer
                asyncResultService.sendCompletedResult(
                    requestId,
                    weapon
                );
            } else {
                logger.warn("武器不存在: name={}, requestId={}", name, requestId);

                // 發送錯誤結果給 Producer
                asyncResultService.sendFailedResult(
                    requestId,
                    "武器不存在: " + name
                );
            }
            
        } catch (Exception e) {
            logger.error("處理根據名稱獲取武器請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤結果給 Producer
                asyncResultService.sendFailedResult(
                    requestId,
                    "獲取武器失敗: " + e.getMessage()
                );
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
    
    /**
     * 監聽 Weapon 根據擁有者獲取請求
     */
    @RabbitListener(queues = "weapon-get-by-owner", concurrency = "2")
    public void handleGetWeaponsByOwner(String messageJson) {
        try {
            logger.info("收到根據擁有者獲取武器請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            String owner = (String) message.getPayload();
            
            logger.info("開始根據擁有者獲取武器: owner={}, requestId={}", owner, requestId);
            
            // 處理請求
            List<Weapon> weapons = weaponService.getWeaponsByOwner(owner).collectList().block();
            
            logger.info("成功獲取武器: owner={}, count={}, requestId={}", owner, weapons.size(), requestId);
            
            // 發送成功結果給 Producer
            asyncResultService.sendCompletedResult(
                requestId,
                weapons
            );
            
        } catch (Exception e) {
            logger.error("處理根據擁有者獲取武器請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤結果給 Producer
                asyncResultService.sendFailedResult(
                    requestId,
                    "獲取武器失敗: " + e.getMessage()
                );
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
    
    /**
     * 監聽 Weapon 保存請求
     */
    @RabbitListener(queues = "weapon-save", concurrency = "2")
    public void handleSaveWeapon(String messageJson) {
        try {
            logger.info("收到保存武器請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            Weapon weapon = objectMapper.convertValue(message.getPayload(), Weapon.class);
            
            logger.info("開始保存武器: name={}, requestId={}", weapon.getName(), requestId);
            
            // 處理請求
            Weapon savedWeapon = weaponService.saveWeapon(weapon).block();
            
            logger.info("成功保存武器: name={}, requestId={}", savedWeapon.getName(), requestId);
            
            // 發送成功結果給 Producer
            asyncResultService.sendCompletedResult(
                requestId,
                savedWeapon
            );
            
        } catch (Exception e) {
            logger.error("處理保存武器請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤結果給 Producer
                asyncResultService.sendFailedResult(
                    requestId,
                    "保存武器失敗: " + e.getMessage()
                );
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
    
    /**
     * 監聽 Weapon 刪除請求
     */
    @RabbitListener(queues = "weapon-delete", concurrency = "2")
    public void handleDeleteWeapon(String messageJson) {
        try {
            logger.info("收到刪除武器請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            String name = (String) message.getPayload();
            
            logger.info("開始刪除武器: name={}, requestId={}", name, requestId);
            
            // 處理請求
            weaponService.deleteWeapon(name);
            
            logger.info("成功刪除武器: name={}, requestId={}", name, requestId);
            
            // 發送成功結果給 Producer
            asyncResultService.sendCompletedResult(
                requestId,
                null
            );
            
        } catch (Exception e) {
            logger.error("處理刪除武器請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤結果給 Producer
                asyncResultService.sendFailedResult(
                    requestId,
                    "刪除武器失敗: " + e.getMessage()
                );
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
    
    /**
     * 監聽 Weapon 刪除所有請求
     */
    @RabbitListener(queues = "weapon-delete-all", concurrency = "2")
    public void handleDeleteAllWeapons(String messageJson) {
        try {
            logger.info("收到刪除所有武器請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            
            logger.info("開始刪除所有武器: requestId={}", requestId);
            
            // 處理請求
            weaponService.deleteAllWeapons();
            
            logger.info("成功刪除所有武器: requestId={}", requestId);
            
            // 發送成功結果給 Producer
            asyncResultService.sendCompletedResult(
                requestId,
                null
            );
            
        } catch (Exception e) {
            logger.error("處理刪除所有武器請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤結果給 Producer
                asyncResultService.sendFailedResult(
                    requestId,
                    "刪除所有武器失敗: " + e.getMessage()
                );
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
    
    /**
     * 監聽 Weapon 存在檢查請求
     */
    @RabbitListener(queues = "weapon-exists", concurrency = "2")
    public void handleWeaponExists(String messageJson) {
        try {
            logger.info("收到檢查武器存在請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            String name = (String) message.getPayload();
            
            logger.info("開始檢查武器存在: name={}, requestId={}", name, requestId);
            
            // 處理請求
            boolean exists = weaponService.weaponExists(name).block();
            
            logger.info("武器存在檢查完成: name={}, exists={}, requestId={}", name, exists, requestId);
            
            // 發送成功結果給 Producer
            asyncResultService.sendCompletedResult(
                requestId,
                exists
            );
            
        } catch (Exception e) {
            logger.error("處理檢查武器存在請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤結果給 Producer
                asyncResultService.sendFailedResult(
                    requestId,
                    "檢查武器存在失敗: " + e.getMessage()
                );
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
}
