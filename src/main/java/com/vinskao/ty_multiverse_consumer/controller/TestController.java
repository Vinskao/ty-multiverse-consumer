package com.vinskao.ty_multiverse_consumer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.vinskao.ty_multiverse_consumer.core.service.AsyncMessageService;
import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.People;
import com.vinskao.ty_multiverse_consumer.module.weapon.domain.vo.Weapon;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private AsyncMessageService asyncMessageService;
    
    @Autowired
    private com.vinskao.ty_multiverse_consumer.module.people.service.PeopleService peopleService;
    
    @Autowired
    private com.vinskao.ty_multiverse_consumer.module.weapon.service.WeaponService weaponService;

    /**
     * 檢查 RabbitMQ 消費者狀態
     */
    @GetMapping("/rabbitmq-status")
    public ResponseEntity<Map<String, Object>> getRabbitMQStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // 檢查 RabbitMQ 是否啟用
        String rabbitmqEnabled = System.getenv("RABBITMQ_ENABLED");
        status.put("rabbitmqEnabled", rabbitmqEnabled != null ? rabbitmqEnabled : "false");
        
        // 檢查環境變數
        status.put("RABBITMQ_HOST", System.getenv("RABBITMQ_HOST"));
        status.put("RABBITMQ_PORT", System.getenv("RABBITMQ_PORT"));
        status.put("RABBITMQ_USERNAME", System.getenv("RABBITMQ_USERNAME"));
        
        // 檢查消費者是否正在運行
        status.put("consumersActive", "需要檢查日誌確認");
        
        return ResponseEntity.ok(status);
    }

    /**
     * 測試發送 People 消息
     */
    @PostMapping("/send-people-message")
    public ResponseEntity<Map<String, String>> sendPeopleMessage() {
        try {
            String requestId = asyncMessageService.sendPeopleGetAllRequest();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "People 消息已發送",
                "requestId", requestId
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "發送失敗: " + e.getMessage()
            ));
        }
    }

    /**
     * 測試發送傷害計算消息
     */
    @PostMapping("/send-damage-calculation")
    public ResponseEntity<Map<String, String>> sendDamageCalculation(@RequestParam String characterName) {
        try {
            String requestId = asyncMessageService.sendDamageCalculationRequest(characterName);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "傷害計算消息已發送",
                "requestId", requestId,
                "characterName", characterName
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "發送失敗: " + e.getMessage()
            ));
        }
    }

    /**
     * 測試創建 People 數據
     */
    @PostMapping("/create-test-people")
    public ResponseEntity<Map<String, String>> createTestPeople() {
        try {
            // 創建測試 People 數據
            People testPeople = new People();
            testPeople.setName("TestCharacter");
            testPeople.setAge(25);
            testPeople.setGender("Male");
            testPeople.setJob("Warrior");
            testPeople.setAttributes("Strong, Brave");
            testPeople.setStateAttributes("Normal");
            
            People savedPeople = peopleService.insertPerson(testPeople);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "測試 People 數據已創建",
                "name", savedPeople.getName(),
                "id", savedPeople.getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "創建失敗: " + e.getMessage()
            ));
        }
    }

    /**
     * 測試創建 Weapon 數據
     */
    @PostMapping("/create-test-weapon")
    public ResponseEntity<Map<String, String>> createTestWeapon() {
        try {
            // 創建測試 Weapon 數據
            Weapon testWeapon = new Weapon();
            testWeapon.setName("TestSword");
            testWeapon.setOwner("TestCharacter");
            testWeapon.setAttributes("Sharp, Magical");
            testWeapon.setBaseDamage(100);
            testWeapon.setBonusDamage(20);
            testWeapon.setBonusAttributes("[\"Fire\", \"Ice\"]");
            testWeapon.setStateAttributes("[\"Burning\", \"Frozen\"]");
            
            Weapon savedWeapon = weaponService.saveWeaponSmart(testWeapon);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "測試 Weapon 數據已創建",
                "name", savedWeapon.getName(),
                "owner", savedWeapon.getOwner()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "創建失敗: " + e.getMessage()
            ));
        }
    }

    /**
     * 模擬 Producer 發送 People 插入消息
     */
    @PostMapping("/simulate-producer-people-insert")
    public ResponseEntity<Map<String, String>> simulateProducerPeopleInsert() {
        try {
            // 模擬 Producer 發送 People 插入消息
            // 這裡會觸發 PeopleConsumer.handleInsertPeople()
            String requestId = asyncMessageService.sendPeopleGetAllRequest();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Producer 已發送 People 插入消息",
                "requestId", requestId,
                "note", "請檢查控制台日誌查看 Consumer 處理情況"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "發送失敗: " + e.getMessage()
            ));
        }
    }

    /**
     * 測試 Consumer 處理 People 插入
     */
    @PostMapping("/test-consumer-people-insert")
    public ResponseEntity<Map<String, String>> testConsumerPeopleInsert() {
        try {
            // 1. 先創建測試數據
            People testPeople = new People();
            testPeople.setName("ConsumerTestCharacter");
            testPeople.setAge(30);
            testPeople.setGender("Female");
            testPeople.setJob("Mage");
            testPeople.setAttributes("Intelligent, Wise");
            testPeople.setStateAttributes("Enchanted");
            
            // 2. 直接保存到數據庫（模擬 Consumer 處理）
            People savedPeople = peopleService.insertPerson(testPeople);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Consumer 測試完成 - People 已插入數據庫",
                "name", savedPeople.getName(),
                "note", "請檢查控制台日誌查看 Consumer 處理情況"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "測試失敗: " + e.getMessage()
            ));
        }
    }

    /**
     * 測試 Consumer 處理 Weapon 保存
     */
    @PostMapping("/test-consumer-weapon-save")
    public ResponseEntity<Map<String, String>> testConsumerWeaponSave() {
        try {
            // 1. 先創建測試數據
            Weapon testWeapon = new Weapon();
            testWeapon.setName("ConsumerTestSword");
            testWeapon.setOwner("ConsumerTestCharacter");
            testWeapon.setAttributes("Mystical, Powerful");
            testWeapon.setBaseDamage(150);
            testWeapon.setBonusDamage(30);
            testWeapon.setBonusAttributes("[\"Lightning\", \"Shadow\"]");
            testWeapon.setStateAttributes("[\"Charged\", \"Dark\"]");
            
            // 2. 直接保存到數據庫（模擬 Consumer 處理）
            Weapon savedWeapon = weaponService.saveWeaponSmart(testWeapon);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Consumer 測試完成 - Weapon 已保存到數據庫",
                "name", savedWeapon.getName(),
                "owner", savedWeapon.getOwner(),
                "note", "請檢查控制台日誌查看 Consumer 處理情況"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "測試失敗: " + e.getMessage()
            ));
        }
    }

    /**
     * 查看數據庫中的 People 數據
     */
    @GetMapping("/view-people-data")
    public ResponseEntity<Map<String, Object>> viewPeopleData() {
        try {
            var peopleList = peopleService.getAllPeopleOptimized();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "count", peopleList.size(),
                "data", peopleList
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "查詢失敗: " + e.getMessage()
            ));
        }
    }

    /**
     * 查看數據庫中的 Weapon 數據
     */
    @GetMapping("/view-weapon-data")
    public ResponseEntity<Map<String, Object>> viewWeaponData() {
        try {
            var weaponList = weaponService.getAllWeapons();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "count", weaponList.size(),
                "data", weaponList
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "查詢失敗: " + e.getMessage()
            ));
        }
    }

    /**
     * 創建測試數據並觸發 Consumer 處理
     */
    @PostMapping("/create-test-data-and-trigger-consumer")
    public ResponseEntity<Map<String, String>> createTestDataAndTriggerConsumer() {
        try {
            // 1. 創建測試 People 數據
            People testPeople1 = new People();
            testPeople1.setName("WarriorHero");
            testPeople1.setAge(28);
            testPeople1.setGender("Male");
            testPeople1.setJob("Warrior");
            testPeople1.setAttributes("Strong, Brave, Loyal");
            testPeople1.setStateAttributes("Normal");
            
            People testPeople2 = new People();
            testPeople2.setName("MageWizard");
            testPeople2.setAge(35);
            testPeople2.setGender("Female");
            testPeople2.setJob("Mage");
            testPeople2.setAttributes("Intelligent, Wise, Mystical");
            testPeople2.setStateAttributes("Enchanted");
            
            // 2. 保存到數據庫
            People savedPeople1 = peopleService.insertPerson(testPeople1);
            People savedPeople2 = peopleService.insertPerson(testPeople2);
            
            // 3. 觸發 Consumer 處理（模擬 Producer 發送消息）
            String requestId = asyncMessageService.sendPeopleGetAllRequest();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "測試數據已創建並觸發 Consumer 處理",
                "createdPeople", savedPeople1.getName() + ", " + savedPeople2.getName(),
                "requestId", requestId,
                "note", "請檢查控制台日誌查看 Consumer 處理的詳細數據"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "創建失敗: " + e.getMessage()
            ));
        }
    }
}
