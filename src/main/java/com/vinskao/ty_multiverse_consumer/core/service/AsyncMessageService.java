package com.vinskao.ty_multiverse_consumer.core.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vinskao.ty_multiverse_consumer.config.RabbitMQConfig;

import java.util.UUID;

@Service
public class AsyncMessageService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 發送獲取所有角色列表的請求
     * 
     * @return 請求ID
     */
    public String sendPeopleGetAllRequest() {
        String requestId = UUID.randomUUID().toString();
        
        // 創建請求消息
        PeopleGetAllRequest request = new PeopleGetAllRequest();
        request.setRequestId(requestId);
        request.setTimestamp(System.currentTimeMillis());
        
        // 發送到 RabbitMQ
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.MAIN_EXCHANGE,
            RabbitMQConfig.PEOPLE_GET_ALL_ROUTING_KEY,
            request
        );
        
        return requestId;
    }

    /**
     * 發送傷害計算請求
     * 
     * @param characterName 角色名稱
     * @return 請求ID
     */
    public String sendDamageCalculationRequest(String characterName) {
        String requestId = UUID.randomUUID().toString();
        
        // 創建請求消息
        DamageCalculationRequest request = new DamageCalculationRequest();
        request.setRequestId(requestId);
        request.setCharacterName(characterName);
        request.setTimestamp(System.currentTimeMillis());
        
        // 發送到 RabbitMQ
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.MAIN_EXCHANGE,
            RabbitMQConfig.PEOPLE_DAMAGE_CALCULATION_ROUTING_KEY,
            request
        );
        
        return requestId;
    }

    // 內部類：獲取所有角色請求
    public static class PeopleGetAllRequest {
        private String requestId;
        private long timestamp;

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    // 內部類：傷害計算請求
    public static class DamageCalculationRequest {
        private String requestId;
        private String characterName;
        private long timestamp;

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getCharacterName() {
            return characterName;
        }

        public void setCharacterName(String characterName) {
            this.characterName = characterName;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
