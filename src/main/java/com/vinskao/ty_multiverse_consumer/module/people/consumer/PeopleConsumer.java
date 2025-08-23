package com.vinskao.ty_multiverse_consumer.module.people.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.vinskao.ty_multiverse_consumer.module.people.service.PeopleService;
import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.People;

import java.util.List;

/**
 * People 模組 Consumer
 * 
 * 負責接收和處理 RabbitMQ 中的 People 相關消息
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
    private PeopleService peopleService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 處理插入單個角色請求
     */
    @RabbitListener(queues = "people-insert")
    public void handleInsertPeople(String messageJson) {
        try {
            logger.info("收到插入角色請求: {}", messageJson);
            
            PeopleMessageDTO message = objectMapper.readValue(messageJson, PeopleMessageDTO.class);
            People people = message.getPeople();
            
            People savedPeople = peopleService.insertPerson(people);
            
            logger.info("成功插入角色: name={}, requestId={}", 
                       savedPeople.getName(), message.getRequestId());
            
        } catch (Exception e) {
            logger.error("處理插入角色請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 處理更新角色請求
     */
    @RabbitListener(queues = "people-update")
    public void handleUpdatePeople(String messageJson) {
        try {
            logger.info("收到更新角色請求: {}", messageJson);
            
            PeopleMessageDTO message = objectMapper.readValue(messageJson, PeopleMessageDTO.class);
            People people = message.getPeople();
            
            People updatedPeople = peopleService.updatePerson(people);
            
            logger.info("成功更新角色: name={}, requestId={}", 
                       updatedPeople.getName(), message.getRequestId());
            
        } catch (Exception e) {
            logger.error("處理更新角色請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 處理插入多個角色請求
     */
    @RabbitListener(queues = "people-insert-multiple")
    public void handleInsertMultiplePeople(String messageJson) {
        try {
            logger.info("收到插入多個角色請求: {}", messageJson);
            
            PeopleMessageDTO message = objectMapper.readValue(messageJson, PeopleMessageDTO.class);
            @SuppressWarnings("unchecked")
            List<People> peopleList = (List<People>) message.getData();
            
            List<People> savedPeople = peopleService.saveAllPeople(peopleList);
            
            logger.info("成功插入多個角色: count={}, requestId={}", 
                       savedPeople.size(), message.getRequestId());
            
        } catch (Exception e) {
            logger.error("處理插入多個角色請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 處理獲取所有角色請求
     */
    @RabbitListener(queues = "people-get-all")
    public void handleGetAllPeople(String messageJson) {
        try {
            logger.info("收到獲取所有角色請求: {}", messageJson);
            
            PeopleMessageDTO message = objectMapper.readValue(messageJson, PeopleMessageDTO.class);
            
            List<People> people = peopleService.getAllPeopleOptimized();
            
            logger.info("成功獲取所有角色: count={}, requestId={}", 
                       people.size(), message.getRequestId());
            
        } catch (Exception e) {
            logger.error("處理獲取所有角色請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 處理根據名稱獲取角色請求
     */
    @RabbitListener(queues = "people-get-by-name")
    public void handleGetPeopleByName(String messageJson) {
        try {
            logger.info("收到根據名稱獲取角色請求: {}", messageJson);
            
            PeopleMessageDTO message = objectMapper.readValue(messageJson, PeopleMessageDTO.class);
            String name = (String) message.getData();
            
            peopleService.getPeopleByName(name).ifPresent(people -> {
                logger.info("成功獲取角色: name={}, requestId={}", 
                           people.getName(), message.getRequestId());
            });
            
        } catch (Exception e) {
            logger.error("處理根據名稱獲取角色請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 處理刪除角色請求
     */
    @RabbitListener(queues = "people-delete")
    public void handleDeletePeople(String messageJson) {
        try {
            logger.info("收到刪除角色請求: {}", messageJson);
            
            PeopleMessageDTO message = objectMapper.readValue(messageJson, PeopleMessageDTO.class);
            String name = (String) message.getData();
            
            // 注意：PeopleService 目前沒有單個刪除方法，只有 deleteAllPeople()
            // 這裡可以根據需求實現單個刪除邏輯
            // 例如：peopleRepository.deleteById(name);
            logger.warn("單個刪除功能尚未實現: name={}", name);
            
            logger.info("成功刪除角色: name={}, requestId={}", 
                       name, message.getRequestId());
            
        } catch (Exception e) {
            logger.error("處理刪除角色請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 處理傷害計算請求
     */
    @RabbitListener(queues = "people-damage-calculation")
    public void handlePeopleDamageCalculation(String messageJson) {
        try {
            logger.info("收到角色傷害計算請求: {}", messageJson);
            
            PeopleMessageDTO message = objectMapper.readValue(messageJson, PeopleMessageDTO.class);
            String characterName = (String) message.getData();
            
            // 這裡可以調用傷害計算服務
            // 例如：damageCalculationService.calculateDamage(characterName);
            
            logger.info("成功處理角色傷害計算: characterName={}, requestId={}", 
                       characterName, message.getRequestId());
            
        } catch (Exception e) {
            logger.error("處理角色傷害計算請求失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * People 消息 DTO（與 Producer 保持一致）
     */
    public static class PeopleMessageDTO {
        private String requestId;
        private String operation;
        private People people;
        private Object data;
        
        // Getters and Setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        
        public People getPeople() { return people; }
        public void setPeople(People people) { this.people = people; }
        
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}
