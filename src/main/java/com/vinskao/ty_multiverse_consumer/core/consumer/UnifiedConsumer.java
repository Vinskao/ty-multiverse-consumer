package com.vinskao.ty_multiverse_consumer.core.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinskao.ty_multiverse_consumer.core.dto.AsyncMessageDTO;
import com.vinskao.ty_multiverse_consumer.core.dto.ProducerResponseDTO;
import com.vinskao.ty_multiverse_consumer.core.service.ConsumerResponseService;
import com.vinskao.ty_multiverse_consumer.module.people.service.PeopleService;
import com.vinskao.ty_multiverse_consumer.module.people.service.WeaponDamageService;
import com.vinskao.ty_multiverse_consumer.module.weapon.service.WeaponService;
import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.People;
import com.vinskao.ty_multiverse_consumer.module.weapon.domain.vo.Weapon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 統一 Consumer
 * 處理所有隊列的消息，使用統一的 AsyncMessageDTO 格式
 */
@Component
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true")
public class UnifiedConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(UnifiedConsumer.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ConsumerResponseService responseService;
    
    @Autowired
    private PeopleService peopleService;
    
    @Autowired
    private WeaponService weaponService;
    
    @Autowired
    private WeaponDamageService weaponDamageService;
    
    // ==================== People 相關隊列 ====================
    
    /**
     * 處理傷害計算請求
     */
    @RabbitListener(queues = "damage-calculation")
    public void handleDamageCalculation(String messageJson) {
        try {
            logger.info("收到傷害計算請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            String characterName = (String) message.getPayload();
            int damage = weaponDamageService.calculateDamageWithWeapon(characterName);
            
            responseService.sendPeopleSuccessResponse(
                message.getRequestId(), 
                "傷害計算完成", 
                damage
            );
            
        } catch (Exception e) {
            logger.error("處理傷害計算請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理獲取所有角色請求
     */
    @RabbitListener(queues = "people-get-all")
    public void handlePeopleGetAll(String messageJson) {
        try {
            logger.info("收到獲取所有角色請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            List<People> peopleList = peopleService.getAllPeopleOptimized();
            
            responseService.sendPeopleSuccessResponse(
                message.getRequestId(), 
                "成功獲取所有角色", 
                peopleList
            );
            
        } catch (Exception e) {
            logger.error("處理獲取所有角色請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理插入角色請求
     */
    @RabbitListener(queues = "people-insert")
    public void handlePeopleInsert(String messageJson) {
        try {
            logger.info("收到插入角色請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            People people = objectMapper.convertValue(message.getPayload(), People.class);
            People savedPeople = peopleService.insertPerson(people);
            
            responseService.sendPeopleSuccessResponse(
                message.getRequestId(), 
                "成功插入角色", 
                savedPeople
            );
            
        } catch (Exception e) {
            logger.error("處理插入角色請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理更新角色請求
     */
    @RabbitListener(queues = "people-update")
    public void handlePeopleUpdate(String messageJson) {
        try {
            logger.info("收到更新角色請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            People people = objectMapper.convertValue(message.getPayload(), People.class);
            People updatedPeople = peopleService.updatePerson(people);
            
            responseService.sendPeopleSuccessResponse(
                message.getRequestId(), 
                "成功更新角色", 
                updatedPeople
            );
            
        } catch (Exception e) {
            logger.error("處理更新角色請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理插入多個角色請求
     */
    @RabbitListener(queues = "people-insert-multiple")
    public void handlePeopleInsertMultiple(String messageJson) {
        try {
            logger.info("收到插入多個角色請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            @SuppressWarnings("unchecked")
            List<People> peopleList = objectMapper.convertValue(message.getPayload(), 
                objectMapper.getTypeFactory().constructCollectionType(List.class, People.class));
            
            List<People> savedPeople = peopleService.saveAllPeople(peopleList);
            
            responseService.sendPeopleSuccessResponse(
                message.getRequestId(), 
                "成功插入多個角色", 
                savedPeople
            );
            
        } catch (Exception e) {
            logger.error("處理插入多個角色請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理根據名稱獲取角色請求
     */
    @RabbitListener(queues = "people-get-by-name")
    public void handlePeopleGetByName(String messageJson) {
        try {
            logger.info("收到根據名稱獲取角色請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            String name = (String) message.getPayload();
            Optional<People> people = peopleService.getPeopleByName(name);
            
            if (people.isPresent()) {
                responseService.sendPeopleSuccessResponse(
                    message.getRequestId(), 
                    "成功獲取角色", 
                    people.get()
                );
            } else {
                responseService.sendPeopleErrorResponse(
                    message.getRequestId(), 
                    "角色不存在", 
                    "NOT_FOUND", 
                    "找不到名稱為 " + name + " 的角色"
                );
            }
            
        } catch (Exception e) {
            logger.error("處理根據名稱獲取角色請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理刪除角色請求
     */
    @RabbitListener(queues = "people-delete")
    public void handlePeopleDelete(String messageJson) {
        try {
            logger.info("收到刪除角色請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            String name = (String) message.getPayload();
            // TODO: 實現單個刪除邏輯
            logger.warn("單個刪除功能尚未實現: name={}", name);
            
            responseService.sendPeopleSuccessResponse(
                message.getRequestId(), 
                "刪除角色請求已接收", 
                null
            );
            
        } catch (Exception e) {
            logger.error("處理刪除角色請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理刪除所有角色請求
     */
    @RabbitListener(queues = "people-delete-all")
    public void handlePeopleDeleteAll(String messageJson) {
        try {
            logger.info("收到刪除所有角色請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            peopleService.deleteAllPeople();
            
            responseService.sendPeopleSuccessResponse(
                message.getRequestId(), 
                "成功刪除所有角色", 
                null
            );
            
        } catch (Exception e) {
            logger.error("處理刪除所有角色請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理傷害計算請求（people-damage-calculation 隊列）
     */
    @RabbitListener(queues = "people-damage-calculation")
    public void handlePeopleDamageCalculation(String messageJson) {
        try {
            logger.info("收到角色傷害計算請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            String characterName = (String) message.getPayload();
            int damage = weaponDamageService.calculateDamageWithWeapon(characterName);
            
            responseService.sendPeopleSuccessResponse(
                message.getRequestId(), 
                "傷害計算完成", 
                damage
            );
            
        } catch (Exception e) {
            logger.error("處理角色傷害計算請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    // ==================== Weapon 相關隊列 ====================
    
    /**
     * 處理獲取所有武器請求
     */
    @RabbitListener(queues = "weapon-get-all")
    public void handleWeaponGetAll(String messageJson) {
        try {
            logger.info("收到獲取所有武器請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            List<Weapon> weapons = weaponService.getAllWeapons();
            
            responseService.sendWeaponSuccessResponse(
                message.getRequestId(), 
                "成功獲取所有武器", 
                weapons
            );
            
        } catch (Exception e) {
            logger.error("處理獲取所有武器請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理根據名稱獲取武器請求
     */
    @RabbitListener(queues = "weapon-get-by-name")
    public void handleWeaponGetByName(String messageJson) {
        try {
            logger.info("收到根據名稱獲取武器請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            String name = (String) message.getPayload();
            Optional<Weapon> weapon = weaponService.getWeaponById(name);
            
            if (weapon.isPresent()) {
                responseService.sendWeaponSuccessResponse(
                    message.getRequestId(), 
                    "成功獲取武器", 
                    weapon.get()
                );
            } else {
                responseService.sendWeaponErrorResponse(
                    message.getRequestId(), 
                    "武器不存在", 
                    "NOT_FOUND", 
                    "找不到名稱為 " + name + " 的武器"
                );
            }
            
        } catch (Exception e) {
            logger.error("處理根據名稱獲取武器請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理根據擁有者獲取武器請求
     */
    @RabbitListener(queues = "weapon-get-by-owner")
    public void handleWeaponGetByOwner(String messageJson) {
        try {
            logger.info("收到根據擁有者獲取武器請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            String owner = (String) message.getPayload();
            List<Weapon> weapons = weaponService.getWeaponsByOwner(owner);
            
            responseService.sendWeaponSuccessResponse(
                message.getRequestId(), 
                "成功獲取武器", 
                weapons
            );
            
        } catch (Exception e) {
            logger.error("處理根據擁有者獲取武器請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理保存武器請求
     */
    @RabbitListener(queues = "weapon-save")
    public void handleWeaponSave(String messageJson) {
        try {
            logger.info("收到保存武器請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            Weapon weapon = objectMapper.convertValue(message.getPayload(), Weapon.class);
            Weapon savedWeapon = weaponService.saveWeapon(weapon);
            
            responseService.sendWeaponSuccessResponse(
                message.getRequestId(), 
                "成功保存武器", 
                savedWeapon
            );
            
        } catch (Exception e) {
            logger.error("處理保存武器請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理刪除武器請求
     */
    @RabbitListener(queues = "weapon-delete")
    public void handleWeaponDelete(String messageJson) {
        try {
            logger.info("收到刪除武器請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            String name = (String) message.getPayload();
            weaponService.deleteWeapon(name);
            
            responseService.sendWeaponSuccessResponse(
                message.getRequestId(), 
                "成功刪除武器", 
                null
            );
            
        } catch (Exception e) {
            logger.error("處理刪除武器請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理刪除所有武器請求
     */
    @RabbitListener(queues = "weapon-delete-all")
    public void handleWeaponDeleteAll(String messageJson) {
        try {
            logger.info("收到刪除所有武器請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            weaponService.deleteAllWeapons();
            
            responseService.sendWeaponSuccessResponse(
                message.getRequestId(), 
                "成功刪除所有武器", 
                null
            );
            
        } catch (Exception e) {
            logger.error("處理刪除所有武器請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理檢查武器存在請求
     */
    @RabbitListener(queues = "weapon-exists")
    public void handleWeaponExists(String messageJson) {
        try {
            logger.info("收到檢查武器存在請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            String name = (String) message.getPayload();
            boolean exists = weaponService.weaponExists(name);
            
            responseService.sendWeaponSuccessResponse(
                message.getRequestId(), 
                "武器存在檢查完成", 
                exists
            );
            
        } catch (Exception e) {
            logger.error("處理檢查武器存在請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理更新武器屬性請求
     */
    @RabbitListener(queues = "weapon-update-attributes")
    public void handleWeaponUpdateAttributes(String messageJson) {
        try {
            logger.info("收到更新武器屬性請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            // TODO: 實現更新武器屬性邏輯
            logger.warn("更新武器屬性功能尚未實現");
            
            responseService.sendWeaponSuccessResponse(
                message.getRequestId(), 
                "更新武器屬性請求已接收", 
                null
            );
            
        } catch (Exception e) {
            logger.error("處理更新武器屬性請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    /**
     * 處理更新武器基礎傷害請求
     */
    @RabbitListener(queues = "weapon-update-base-damage")
    public void handleWeaponUpdateBaseDamage(String messageJson) {
        try {
            logger.info("收到更新武器基礎傷害請求: {}", messageJson);
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            // TODO: 實現更新武器基礎傷害邏輯
            logger.warn("更新武器基礎傷害功能尚未實現");
            
            responseService.sendWeaponSuccessResponse(
                message.getRequestId(), 
                "更新武器基礎傷害請求已接收", 
                null
            );
            
        } catch (Exception e) {
            logger.error("處理更新武器基礎傷害請求失敗: {}", e.getMessage(), e);
            handleError(messageJson, e);
        }
    }
    
    // ==================== 錯誤處理 ====================
    
    /**
     * 統一錯誤處理方法
     */
    private void handleError(String messageJson, Exception e) {
        try {
            AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            
            // 根據端點判斷發送到哪個回應隊列
            if (message.getEndpoint().startsWith("/people")) {
                responseService.sendPeopleErrorResponse(
                    message.getRequestId(),
                    "處理請求失敗: " + e.getMessage(),
                    "INTERNAL_ERROR",
                    e.toString()
                );
            } else if (message.getEndpoint().startsWith("/weapon")) {
                responseService.sendWeaponErrorResponse(
                    message.getRequestId(),
                    "處理請求失敗: " + e.getMessage(),
                    "INTERNAL_ERROR",
                    e.toString()
                );
            } else {
                // 默認發送到 people 回應隊列
                responseService.sendPeopleErrorResponse(
                    message.getRequestId(),
                    "處理請求失敗: " + e.getMessage(),
                    "INTERNAL_ERROR",
                    e.toString()
                );
            }
            
        } catch (Exception ex) {
            logger.error("處理錯誤回應失敗: {}", ex.getMessage(), ex);
        }
    }
}
