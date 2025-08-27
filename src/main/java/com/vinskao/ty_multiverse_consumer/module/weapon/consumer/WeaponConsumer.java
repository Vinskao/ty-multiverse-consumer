package com.vinskao.ty_multiverse_consumer.module.weapon.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.vinskao.ty_multiverse_consumer.module.weapon.service.WeaponService;
import com.vinskao.ty_multiverse_consumer.module.weapon.domain.vo.Weapon;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.io.IOException;

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
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
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
            
            // 發送回傳消息
            sendResponse(message.getRequestId(), "success", "成功獲取所有武器", weapons);
            
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
                
                // 發送回傳消息
                sendResponse(message.getRequestId(), "success", "成功獲取武器", weapon);
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
            
            // 發送回傳消息
            sendResponse(message.getRequestId(), "success", "成功獲取擁有者武器", weapons);
            
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
     * 處理批量插入武器請求
     */
    @RabbitListener(queues = "weapon-insert-multiple")
    public void handleInsertMultipleWeapons(String messageJson) {
        try {
            logger.info("收到批量插入武器請求: {}", messageJson);
            
            WeaponMessageDTO message = objectMapper.readValue(messageJson, WeaponMessageDTO.class);
            
            // 正確地將 data 轉換為 List<Weapon>
            List<Weapon> weaponList;
            if (message.getData() instanceof List) {
                // 如果 data 已經是 List，需要將每個元素轉換為 Weapon 對象
                @SuppressWarnings("unchecked")
                List<Object> dataList = (List<Object>) message.getData();
                weaponList = new ArrayList<>();
                for (Object item : dataList) {
                    if (item instanceof Weapon) {
                        weaponList.add((Weapon) item);
                    } else {
                        // 將 LinkedHashMap 轉換為 Weapon 對象
                        // 先處理數組類型的字段，將其轉換為 JSON 字符串
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> weaponMap = (Map<String, Object>) item;
                            
                            try {
                                // 直接轉換，因為 bonusAttributes 和 stateAttributes 現在是 List<String> 類型
                                Weapon weapon = objectMapper.convertValue(weaponMap, Weapon.class);
                                weaponList.add(weapon);
                            } catch (Exception e) {
                                logger.error("處理武器數據時發生錯誤: {}", e.getMessage(), e);
                            }
                        } else {
                            Weapon weapon = objectMapper.convertValue(item, Weapon.class);
                            weaponList.add(weapon);
                        }
                    }
                }
            } else {
                // 如果 data 不是 List，嘗試直接轉換
                weaponList = objectMapper.convertValue(message.getData(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Weapon.class));
            }
            
            List<Weapon> savedWeapons = new ArrayList<>();
            for (Weapon weapon : weaponList) {
                Weapon savedWeapon = weaponService.saveWeaponSmart(weapon);
                savedWeapons.add(savedWeapon);
            }
            
            logger.info("成功批量插入武器: count={}, requestId={}", 
                       savedWeapons.size(), message.getRequestId());
            
            // 發送回傳消息
            sendResponse(message.getRequestId(), "success", "成功批量插入武器", savedWeapons);
            
        } catch (Exception e) {
            logger.error("處理批量插入武器請求失敗: {}", e.getMessage(), e);
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
     * 發送回傳消息
     */
    private void sendResponse(String requestId, String status, String message, Object data) {
        try {
            WeaponResponseDTO response = new WeaponResponseDTO();
            response.setRequestId(requestId);
            response.setStatus(status);
            response.setMessage(message);
            response.setData(data);
            response.setTimestamp(System.currentTimeMillis());
            response.setErrorCode(null);
            response.setErrorDetails(null);
            
            String responseJson = objectMapper.writeValueAsString(response);
            rabbitTemplate.convertAndSend("weapon-response", "weapon.response", responseJson);
            
            logger.info("已發送回傳消息: requestId={}, status={}", requestId, status);
        } catch (Exception e) {
            logger.error("發送回傳消息失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Weapon 回傳消息 DTO
     * 與 Producer 端的 ProducerResponseDTO 保持一致
     */
    public static class WeaponResponseDTO {
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
