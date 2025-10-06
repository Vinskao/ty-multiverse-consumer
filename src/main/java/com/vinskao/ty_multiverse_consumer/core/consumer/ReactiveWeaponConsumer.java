package com.vinskao.ty_multiverse_consumer.core.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinskao.ty_multiverse_consumer.config.RabbitMQConfig;
import com.vinskao.ty_multiverse_consumer.core.dto.AsyncMessageDTO;
import com.vinskao.ty_multiverse_consumer.core.service.AsyncResultService;
import com.vinskao.ty_multiverse_consumer.module.weapon.domain.vo.Weapon;
import com.vinskao.ty_multiverse_consumer.module.weapon.service.WeaponService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.Receiver;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 完全 Reactive Weapon Consumer
 * 
 * 使用 Reactor RabbitMQ 實現端到端非阻塞消息處理
 * 與 R2DBC 連線池 (max-size=5) 協調的背壓控制
 * 
 * @author TY Backend Team
 * @version 2.0
 * @since 2024
 */
@Component
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true")
public class ReactiveWeaponConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveWeaponConsumer.class);

    @Autowired
    private Receiver reactiveReceiver;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WeaponService weaponService;

    @Autowired
    private AsyncResultService asyncResultService;

    /**
     * 啟動所有 reactive 消費者
     */
    @PostConstruct
    public void startConsumers() {
        logger.info("🚀 啟動 Reactive Weapon Consumer...");
        
        // 啟動各個隊列的消費者
        startGetAllWeaponsConsumer();
        startGetWeaponByNameConsumer();
        startGetWeaponsByOwnerConsumer();
        startSaveWeaponConsumer();
        startCheckWeaponExistsConsumer();
        
        logger.info("✅ Reactive Weapon Consumer 啟動完成");
    }

    /**
     * Weapon Get-All 消費者
     */
    private void startGetAllWeaponsConsumer() {
        reactiveReceiver
            .consumeManualAck(RabbitMQConfig.WEAPON_GET_ALL_QUEUE, new ConsumeOptions().qos(2))
            .flatMap(this::handleGetAllWeapons, 2)
            .doOnError(error -> logger.error("❌ Weapon Get-All 消費者發生錯誤", error))
            .retry()
            .subscribe();
        
        logger.info("📡 啟動 Weapon Get-All Reactive Consumer (concurrency=2, prefetch=2)");
    }

    /**
     * Weapon Get-By-Name 消費者
     */
    private void startGetWeaponByNameConsumer() {
        reactiveReceiver
            .consumeManualAck(RabbitMQConfig.WEAPON_GET_BY_NAME_QUEUE, new ConsumeOptions().qos(2))
            .flatMap(this::handleGetWeaponByName, 2)
            .doOnError(error -> logger.error("❌ Weapon Get-By-Name 消費者發生錯誤", error))
            .retry()
            .subscribe();
        
        logger.info("📡 啟動 Weapon Get-By-Name Reactive Consumer (concurrency=2)");
    }

    /**
     * Weapon Get-By-Owner 消費者
     */
    private void startGetWeaponsByOwnerConsumer() {
        reactiveReceiver
            .consumeManualAck(RabbitMQConfig.WEAPON_GET_BY_OWNER_QUEUE, new ConsumeOptions().qos(2))
            .flatMap(this::handleGetWeaponsByOwner, 2)
            .doOnError(error -> logger.error("❌ Weapon Get-By-Owner 消費者發生錯誤", error))
            .retry()
            .subscribe();
        
        logger.info("📡 啟動 Weapon Get-By-Owner Reactive Consumer (concurrency=2)");
    }

    /**
     * Weapon Save 消費者
     */
    private void startSaveWeaponConsumer() {
        reactiveReceiver
            .consumeManualAck(RabbitMQConfig.WEAPON_SAVE_QUEUE, new ConsumeOptions().qos(1))
            .flatMap(this::handleSaveWeapon, 1) // 寫操作序列化處理
            .doOnError(error -> logger.error("❌ Weapon Save 消費者發生錯誤", error))
            .retry()
            .subscribe();
        
        logger.info("📡 啟動 Weapon Save Reactive Consumer (concurrency=1)");
    }

    /**
     * Weapon Exists 消費者
     */
    private void startCheckWeaponExistsConsumer() {
        reactiveReceiver
            .consumeManualAck(RabbitMQConfig.WEAPON_EXISTS_QUEUE, new ConsumeOptions().qos(3))
            .flatMap(this::handleCheckWeaponExists, 3) // 輕量級操作，可以更高並發
            .doOnError(error -> logger.error("❌ Weapon Exists 消費者發生錯誤", error))
            .retry()
            .subscribe();
        
        logger.info("📡 啟動 Weapon Exists Reactive Consumer (concurrency=3)");
    }

    /**
     * 處理 Get-All Weapons 請求 - 完全 reactive
     */
    private Mono<Void> handleGetAllWeapons(AcknowledgableDelivery delivery) {
        String messageJson = new String(delivery.getBody());
        
        return Mono.fromCallable(() -> {
                logger.info("🎯 收到 Weapon Get-All 請求: {}", messageJson);
                return objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            })
            .flatMap(message -> {
                String requestId = message.getRequestId();
                logger.info("📝 處理請求: requestId={}", requestId);
                
                return weaponService.getAllWeapons()
                    .collectList()
                    .flatMap(weaponList -> {
                        logger.info("✅ 查詢完成: 共 {} 個武器, requestId={}", weaponList.size(), requestId);
                        return asyncResultService.sendCompletedResultReactive(requestId, weaponList);
                    })
                    .doOnSuccess(v -> {
                        logger.info("🎉 Weapon Get-All 處理完成: requestId={}", requestId);
                        delivery.ack();
                    })
                    .doOnError(error -> {
                        logger.error("❌ Weapon Get-All 處理失敗: requestId={}, error={}", requestId, error.getMessage());
                        
                        asyncResultService.sendFailedResultReactive(requestId, "獲取武器列表失敗: " + error.getMessage())
                            .doFinally(signalType -> delivery.nack(false))
                            .subscribe();
                    });
            })
            .onErrorResume(parseError -> {
                logger.error("❌ 無法解析消息: {}, error={}", messageJson, parseError.getMessage());
                delivery.nack(false);
                return Mono.empty();
            })
            .then();
    }

    /**
     * 處理 Get-By-Name Weapon 請求 - 完全 reactive
     */
    private Mono<Void> handleGetWeaponByName(AcknowledgableDelivery delivery) {
        String messageJson = new String(delivery.getBody());
        
        return Mono.fromCallable(() -> {
                logger.info("🎯 收到 Weapon Get-By-Name 請求: {}", messageJson);
                return objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            })
            .flatMap(message -> {
                String requestId = message.getRequestId();
                String name = (String) message.getPayload();
                logger.info("📝 處理請求: name={}, requestId={}", name, requestId);
                
                return weaponService.getWeaponById(name)
                    .flatMap(weapon -> {
                        logger.info("✅ 查詢成功: name={}, requestId={}", name, requestId);
                        return asyncResultService.sendCompletedResultReactive(requestId, weapon);
                    })
                    .switchIfEmpty(
                        Mono.defer(() -> {
                            logger.warn("⚠️ 武器不存在: name={}, requestId={}", name, requestId);
                            return asyncResultService.sendFailedResultReactive(requestId, "武器不存在: " + name);
                        })
                    )
                    .doOnSuccess(v -> {
                        logger.info("🎉 Weapon Get-By-Name 處理完成: requestId={}", requestId);
                        delivery.ack();
                    })
                    .doOnError(error -> {
                        logger.error("❌ Weapon Get-By-Name 處理失敗: requestId={}, error={}", requestId, error.getMessage());
                        
                        asyncResultService.sendFailedResultReactive(requestId, "獲取武器失敗: " + error.getMessage())
                            .doFinally(signalType -> delivery.nack(false))
                            .subscribe();
                    });
            })
            .onErrorResume(parseError -> {
                logger.error("❌ 無法解析消息: {}, error={}", messageJson, parseError.getMessage());
                delivery.nack(false);
                return Mono.empty();
            })
            .then();
    }

    /**
     * 處理 Get-By-Owner Weapons 請求 - 完全 reactive
     */
    private Mono<Void> handleGetWeaponsByOwner(AcknowledgableDelivery delivery) {
        String messageJson = new String(delivery.getBody());
        
        return Mono.fromCallable(() -> {
                logger.info("🎯 收到 Weapon Get-By-Owner 請求: {}", messageJson);
                return objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            })
            .flatMap(message -> {
                String requestId = message.getRequestId();
                String owner = (String) message.getPayload();
                logger.info("📝 處理請求: owner={}, requestId={}", owner, requestId);
                
                return weaponService.getWeaponsByOwner(owner)
                    .collectList()
                    .flatMap(weaponList -> {
                        logger.info("✅ 查詢完成: owner={}, 共 {} 個武器, requestId={}", owner, weaponList.size(), requestId);
                        return asyncResultService.sendCompletedResultReactive(requestId, weaponList);
                    })
                    .doOnSuccess(v -> {
                        logger.info("🎉 Weapon Get-By-Owner 處理完成: requestId={}", requestId);
                        delivery.ack();
                    })
                    .doOnError(error -> {
                        logger.error("❌ Weapon Get-By-Owner 處理失敗: requestId={}, error={}", requestId, error.getMessage());
                        
                        asyncResultService.sendFailedResultReactive(requestId, "獲取武器列表失敗: " + error.getMessage())
                            .doFinally(signalType -> delivery.nack(false))
                            .subscribe();
                    });
            })
            .onErrorResume(parseError -> {
                logger.error("❌ 無法解析消息: {}, error={}", messageJson, parseError.getMessage());
                delivery.nack(false);
                return Mono.empty();
            })
            .then();
    }

    /**
     * 處理 Save Weapon 請求 - 完全 reactive
     */
    private Mono<Void> handleSaveWeapon(AcknowledgableDelivery delivery) {
        String messageJson = new String(delivery.getBody());
        
        return Mono.fromCallable(() -> {
                logger.info("🎯 收到 Weapon Save 請求: {}", messageJson);
                return objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            })
            .flatMap(message -> {
                String requestId = message.getRequestId();
                Weapon weapon = objectMapper.convertValue(message.getPayload(), Weapon.class);
                logger.info("📝 處理請求: weapon={}, requestId={}", weapon.getName(), requestId);
                
                return weaponService.saveWeapon(weapon)
                    .flatMap(savedWeapon -> {
                        logger.info("✅ 保存成功: weapon={}, requestId={}", savedWeapon.getName(), requestId);
                        return asyncResultService.sendCompletedResultReactive(requestId, savedWeapon);
                    })
                    .doOnSuccess(v -> {
                        logger.info("🎉 Weapon Save 處理完成: requestId={}", requestId);
                        delivery.ack();
                    })
                    .doOnError(error -> {
                        logger.error("❌ Weapon Save 處理失敗: requestId={}, error={}", requestId, error.getMessage());
                        
                        asyncResultService.sendFailedResultReactive(requestId, "保存武器失敗: " + error.getMessage())
                            .doFinally(signalType -> delivery.nack(false))
                            .subscribe();
                    });
            })
            .onErrorResume(parseError -> {
                logger.error("❌ 無法解析消息: {}, error={}", messageJson, parseError.getMessage());
                delivery.nack(false);
                return Mono.empty();
            })
            .then();
    }

    /**
     * 處理 Check Weapon Exists 請求 - 完全 reactive
     */
    private Mono<Void> handleCheckWeaponExists(AcknowledgableDelivery delivery) {
        String messageJson = new String(delivery.getBody());
        
        return Mono.fromCallable(() -> {
                logger.info("🎯 收到 Weapon Exists 請求: {}", messageJson);
                return objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            })
            .flatMap(message -> {
                String requestId = message.getRequestId();
                String name = (String) message.getPayload();
                logger.info("📝 處理請求: name={}, requestId={}", name, requestId);
                
                return weaponService.weaponExists(name)
                    .flatMap(exists -> {
                        logger.info("✅ 檢查完成: name={}, exists={}, requestId={}", name, exists, requestId);
                        return asyncResultService.sendCompletedResultReactive(requestId, exists);
                    })
                    .doOnSuccess(v -> {
                        logger.info("🎉 Weapon Exists 處理完成: requestId={}", requestId);
                        delivery.ack();
                    })
                    .doOnError(error -> {
                        logger.error("❌ Weapon Exists 處理失敗: requestId={}, error={}", requestId, error.getMessage());
                        
                        asyncResultService.sendFailedResultReactive(requestId, "檢查武器存在失敗: " + error.getMessage())
                            .doFinally(signalType -> delivery.nack(false))
                            .subscribe();
                    });
            })
            .onErrorResume(parseError -> {
                logger.error("❌ 無法解析消息: {}, error={}", messageJson, parseError.getMessage());
                delivery.nack(false);
                return Mono.empty();
            })
            .then();
    }

    @PreDestroy
    public void shutdown() {
        logger.info("🛑 關閉 Reactive Weapon Consumer...");
        // Receiver 會自動關閉連接
    }
}
