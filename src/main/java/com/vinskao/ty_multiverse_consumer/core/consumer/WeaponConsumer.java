package com.vinskao.ty_multiverse_consumer.core.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.vinskao.ty_multiverse_consumer.core.dto.AsyncMessageDTO;
import com.vinskao.ty_multiverse_consumer.core.service.ConsumerResponseService;
import com.vinskao.ty_multiverse_consumer.module.weapon.domain.vo.Weapon;
import com.vinskao.ty_multiverse_consumer.module.weapon.service.WeaponService;

import java.util.List;
import java.util.Optional;

/**
 * Weapon 請求處理 Consumer
 * 
 * 負責處理 Weapon 相關的 RabbitMQ 請求
 * 只在 RabbitMQ 啟用時生效
 * 
 * @author TY Backend Team
 * @version 1.0
 * @since 2024
 */
@Component
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true")
public class WeaponConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(WeaponConsumer.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private WeaponService weaponService;
    
    @Autowired
    private ConsumerResponseService consumerResponseService;
    
    /**
     * 監聽 Weapon 獲取所有請求
     */
    @RabbitListener(queues = "weapon-get-all")
    public void handleGetAllWeapons(String messageJson) {
        try {
            logger.info("收到獲取所有武器請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            
            logger.info("開始獲取所有武器: requestId={}", requestId);
            
            // 處理請求
            List<Weapon> weapons = weaponService.getAllWeapons();
            
            logger.info("成功獲取所有武器: count={}, requestId={}", weapons.size(), requestId);
            
            // 發送成功回應給 Producer
            consumerResponseService.sendWeaponSuccessResponse(
                requestId,
                "武器列表獲取成功",
                weapons
            );
            
        } catch (Exception e) {
            logger.error("處理獲取所有武器請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤回應給 Producer
                consumerResponseService.sendWeaponErrorResponse(
                    requestId,
                    "獲取武器列表失敗",
                    "WEAPON_GET_ALL_ERROR",
                    e.getMessage()
                );
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
    
    /**
     * 監聽 Weapon 根據名稱獲取請求
     */
    @RabbitListener(queues = "weapon-get-by-name")
    public void handleGetWeaponByName(String messageJson) {
        try {
            logger.info("收到根據名稱獲取武器請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            String name = (String) message.getPayload();
            
            logger.info("開始根據名稱獲取武器: name={}, requestId={}", name, requestId);
            
            // 處理請求
            Optional<Weapon> weaponOptional = weaponService.getWeaponById(name);
            
            if (weaponOptional.isPresent()) {
                Weapon weapon = weaponOptional.get();
                logger.info("成功獲取武器: name={}, requestId={}", name, requestId);
                
                // 發送成功回應給 Producer
                consumerResponseService.sendWeaponSuccessResponse(
                    requestId,
                    "武器獲取成功",
                    weapon
                );
            } else {
                logger.warn("武器不存在: name={}, requestId={}", name, requestId);
                
                // 發送錯誤回應給 Producer
                consumerResponseService.sendWeaponErrorResponse(
                    requestId,
                    "武器不存在",
                    "WEAPON_NOT_FOUND",
                    "武器名稱: " + name
                );
            }
            
        } catch (Exception e) {
            logger.error("處理根據名稱獲取武器請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤回應給 Producer
                consumerResponseService.sendWeaponErrorResponse(
                    requestId,
                    "獲取武器失敗",
                    "WEAPON_GET_BY_NAME_ERROR",
                    e.getMessage()
                );
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
    
    /**
     * 監聽 Weapon 根據擁有者獲取請求
     */
    @RabbitListener(queues = "weapon-get-by-owner")
    public void handleGetWeaponsByOwner(String messageJson) {
        try {
            logger.info("收到根據擁有者獲取武器請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            String owner = (String) message.getPayload();
            
            logger.info("開始根據擁有者獲取武器: owner={}, requestId={}", owner, requestId);
            
            // 處理請求
            List<Weapon> weapons = weaponService.getWeaponsByOwner(owner);
            
            logger.info("成功獲取武器: count={}, owner={}, requestId={}", weapons.size(), owner, requestId);
            
            // 發送成功回應給 Producer
            consumerResponseService.sendWeaponSuccessResponse(
                requestId,
                "武器獲取成功",
                weapons
            );
            
        } catch (Exception e) {
            logger.error("處理根據擁有者獲取武器請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤回應給 Producer
                consumerResponseService.sendWeaponErrorResponse(
                    requestId,
                    "獲取武器失敗",
                    "WEAPON_GET_BY_OWNER_ERROR",
                    e.getMessage()
                );
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
    
    /**
     * 監聽 Weapon 保存請求
     */
    @RabbitListener(queues = "weapon-save")
    public void handleSaveWeapon(String messageJson) {
        try {
            logger.info("收到保存武器請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            
            // 將 payload 轉換為 Weapon 對象
            Weapon weapon = objectMapper.convertValue(message.getPayload(), Weapon.class);
            
            logger.info("開始保存武器: name={}, requestId={}", weapon.getName(), requestId);
            
            // 處理請求
            Weapon savedWeapon = weaponService.saveWeapon(weapon);
            
            logger.info("成功保存武器: name={}, requestId={}", savedWeapon.getName(), requestId);
            
            // 發送成功回應給 Producer
            consumerResponseService.sendWeaponSuccessResponse(
                requestId,
                "武器保存成功",
                savedWeapon
            );
            
        } catch (Exception e) {
            logger.error("處理保存武器請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤回應給 Producer
                consumerResponseService.sendWeaponErrorResponse(
                    requestId,
                    "保存武器失敗",
                    "WEAPON_SAVE_ERROR",
                    e.getMessage()
                );
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
    
    /**
     * 監聽 Weapon 刪除請求
     */
    @RabbitListener(queues = "weapon-delete")
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
            
            // 發送成功回應給 Producer
            consumerResponseService.sendWeaponSuccessResponse(
                requestId,
                "武器刪除成功",
                null
            );
            
        } catch (Exception e) {
            logger.error("處理刪除武器請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤回應給 Producer
                consumerResponseService.sendWeaponErrorResponse(
                    requestId,
                    "刪除武器失敗",
                    "WEAPON_DELETE_ERROR",
                    e.getMessage()
                );
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
    
    /**
     * 監聽 Weapon 刪除所有請求
     */
    @RabbitListener(queues = "weapon-delete-all")
    public void handleDeleteAllWeapons(String messageJson) {
        try {
            logger.info("收到刪除所有武器請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            
            logger.info("開始刪除所有武器: requestId={}", requestId);
            
            // 處理請求
            weaponService.deleteAllWeapons();
            
            logger.info("成功刪除所有武器: requestId={}", requestId);
            
            // 發送成功回應給 Producer
            consumerResponseService.sendWeaponSuccessResponse(
                requestId,
                "所有武器刪除成功",
                null
            );
            
        } catch (Exception e) {
            logger.error("處理刪除所有武器請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤回應給 Producer
                consumerResponseService.sendWeaponErrorResponse(
                    requestId,
                    "刪除所有武器失敗",
                    "WEAPON_DELETE_ALL_ERROR",
                    e.getMessage()
                );
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
    
    /**
     * 監聽 Weapon 存在檢查請求
     */
    @RabbitListener(queues = "weapon-exists")
    public void handleWeaponExists(String messageJson) {
        try {
            logger.info("收到檢查武器存在請求: {}", messageJson);
            
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            String requestId = message.getRequestId();
            String name = (String) message.getPayload();
            
            logger.info("開始檢查武器存在: name={}, requestId={}", name, requestId);
            
            // 處理請求
            boolean exists = weaponService.weaponExists(name);
            
            logger.info("武器存在檢查完成: name={}, exists={}, requestId={}", name, exists, requestId);
            
            // 發送成功回應給 Producer
            consumerResponseService.sendWeaponSuccessResponse(
                requestId,
                "武器存在檢查完成",
                exists
            );
            
        } catch (Exception e) {
            logger.error("處理檢查武器存在請求失敗: {}", e.getMessage(), e);
            
            // 嘗試解析請求ID
            try {
                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                
                // 發送錯誤回應給 Producer
                consumerResponseService.sendWeaponErrorResponse(
                    requestId,
                    "檢查武器存在失敗",
                    "WEAPON_EXISTS_ERROR",
                    e.getMessage()
                );
                
            } catch (Exception parseError) {
                logger.error("無法解析請求ID，無法發送錯誤回應: {}", parseError.getMessage());
            }
        }
    }
}
