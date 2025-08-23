package com.vinskao.ty_multiverse_consumer.module.weapon.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.vinskao.ty_multiverse_consumer.module.weapon.service.WeaponService;
import com.vinskao.ty_multiverse_consumer.module.weapon.domain.vo.Weapon;

import java.util.List;
import java.util.Map;

/**
 * Weapon 模組 Consumer
 * 
 * 負責接收和處理 RabbitMQ 中的 Weapon 相關消息
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
    private WeaponService weaponService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 處理獲取所有武器請求
     */
    @RabbitListener(queues = "weapon-get-all")
    public void handleGetAllWeapons(String messageJson) {
        try {
            logger.info("收到獲取所有武器請求: {}", messageJson);
            
            WeaponMessageDTO message = objectMapper.readValue(messageJson, WeaponMessageDTO.class);
            
            List<Weapon> weapons = weaponService.getAllWeapons();
            
            logger.info("成功獲取所有武器: count={}, requestId={}", 
                       weapons.size(), message.getRequestId());
            
        } catch (Exception e) {
            logger.error("處理獲取所有武器請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 處理根據名稱獲取武器請求
     */
    @RabbitListener(queues = "weapon-get-by-name")
    public void handleGetWeaponByName(String messageJson) {
        try {
            logger.info("收到根據名稱獲取武器請求: {}", messageJson);
            
            WeaponMessageDTO message = objectMapper.readValue(messageJson, WeaponMessageDTO.class);
            String name = (String) message.getData();
            
            weaponService.getWeaponById(name).ifPresent(weapon -> {
                logger.info("成功獲取武器: name={}, requestId={}", 
                           weapon.getName(), message.getRequestId());
            });
            
        } catch (Exception e) {
            logger.error("處理根據名稱獲取武器請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 處理根據擁有者獲取武器請求
     */
    @RabbitListener(queues = "weapon-get-by-owner")
    public void handleGetWeaponsByOwner(String messageJson) {
        try {
            logger.info("收到根據擁有者獲取武器請求: {}", messageJson);
            
            WeaponMessageDTO message = objectMapper.readValue(messageJson, WeaponMessageDTO.class);
            String owner = (String) message.getData();
            
            List<Weapon> weapons = weaponService.getWeaponsByOwner(owner);
            
            logger.info("成功獲取擁有者武器: owner={}, count={}, requestId={}", 
                       owner, weapons.size(), message.getRequestId());
            
        } catch (Exception e) {
            logger.error("處理根據擁有者獲取武器請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 處理保存武器請求
     */
    @RabbitListener(queues = "weapon-save")
    public void handleSaveWeapon(String messageJson) {
        try {
            logger.info("收到保存武器請求: {}", messageJson);
            
            WeaponMessageDTO message = objectMapper.readValue(messageJson, WeaponMessageDTO.class);
            Weapon weapon = message.getWeapon();
            
            Weapon savedWeapon = weaponService.saveWeaponSmart(weapon);
            
            logger.info("成功保存武器: name={}, requestId={}", 
                       savedWeapon.getName(), message.getRequestId());
            
        } catch (Exception e) {
            logger.error("處理保存武器請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 處理刪除武器請求
     */
    @RabbitListener(queues = "weapon-delete")
    public void handleDeleteWeapon(String messageJson) {
        try {
            logger.info("收到刪除武器請求: {}", messageJson);
            
            WeaponMessageDTO message = objectMapper.readValue(messageJson, WeaponMessageDTO.class);
            String name = (String) message.getData();
            
            weaponService.deleteWeapon(name);
            
            logger.info("成功刪除武器: name={}, requestId={}", 
                       name, message.getRequestId());
            
        } catch (Exception e) {
            logger.error("處理刪除武器請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 處理刪除所有武器請求
     */
    @RabbitListener(queues = "weapon-delete-all")
    public void handleDeleteAllWeapons(String messageJson) {
        try {
            logger.info("收到刪除所有武器請求: {}", messageJson);
            
            WeaponMessageDTO message = objectMapper.readValue(messageJson, WeaponMessageDTO.class);
            
            weaponService.deleteAllWeapons();
            
            logger.info("成功刪除所有武器: requestId={}", message.getRequestId());
            
        } catch (Exception e) {
            logger.error("處理刪除所有武器請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 處理檢查武器存在請求
     */
    @RabbitListener(queues = "weapon-exists")
    public void handleWeaponExists(String messageJson) {
        try {
            logger.info("收到檢查武器存在請求: {}", messageJson);
            
            WeaponMessageDTO message = objectMapper.readValue(messageJson, WeaponMessageDTO.class);
            String name = (String) message.getData();
            
            boolean exists = weaponService.weaponExists(name);
            
            logger.info("成功檢查武器存在: name={}, exists={}, requestId={}", 
                       name, exists, message.getRequestId());
            
        } catch (Exception e) {
            logger.error("處理檢查武器存在請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 處理更新武器屬性請求
     */
    @RabbitListener(queues = "weapon-update-attributes")
    public void handleUpdateWeaponAttributes(String messageJson) {
        try {
            logger.info("收到更新武器屬性請求: {}", messageJson);
            
            WeaponMessageDTO message = objectMapper.readValue(messageJson, WeaponMessageDTO.class);
            @SuppressWarnings("unchecked")
            Map<String, String> data = (Map<String, String>) message.getData();
            
            String name = data.get("name");
            String attributes = data.get("attributes");
            
            Weapon weapon = weaponService.getWeaponById(name)
                    .orElseThrow(() -> new RuntimeException("Weapon not found: " + name));
            weapon.setAttributes(attributes);
            
            Weapon updatedWeapon = weaponService.updateWeaponAttributes(name, weapon);
            
            logger.info("成功更新武器屬性: name={}, requestId={}", 
                       updatedWeapon.getName(), message.getRequestId());
            
        } catch (Exception e) {
            logger.error("處理更新武器屬性請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 處理更新武器基礎傷害請求
     */
    @RabbitListener(queues = "weapon-update-base-damage")
    public void handleUpdateWeaponBaseDamage(String messageJson) {
        try {
            logger.info("收到更新武器基礎傷害請求: {}", messageJson);
            
            WeaponMessageDTO message = objectMapper.readValue(messageJson, WeaponMessageDTO.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.getData();
            
            String name = (String) data.get("name");
            Integer baseDamage = (Integer) data.get("baseDamage");
            
            Weapon updatedWeapon = weaponService.updateWeaponBaseDamage(name, baseDamage);
            
            logger.info("成功更新武器基礎傷害: name={}, baseDamage={}, requestId={}", 
                       updatedWeapon.getName(), baseDamage, message.getRequestId());
            
        } catch (Exception e) {
            logger.error("處理更新武器基礎傷害請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Weapon 消息 DTO（與 Producer 保持一致）
     */
    public static class WeaponMessageDTO {
        private String requestId;
        private String operation;
        private Weapon weapon;
        private Object data;
        
        // Getters and Setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        
        public Weapon getWeapon() { return weapon; }
        public void setWeapon(Weapon weapon) { this.weapon = weapon; }
        
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}
