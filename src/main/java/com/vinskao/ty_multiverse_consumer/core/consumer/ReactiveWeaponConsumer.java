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
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.util.retry.Retry;
import java.time.Duration;

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
@ConditionalOnProperty(name = "spring.rabbitmq.reactive.enabled", havingValue = "true", matchIfMissing = true)
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

    // 用於管理所有消費者的訂閱
    private final Disposable.Composite subscriptions = Disposables.composite();

    // 預設重試策略：指數退避，最大重試 3 次，初始等待 2 秒
    private final Retry defaultRetry = Retry.backoff(3, Duration.ofSeconds(2))
            .maxBackoff(Duration.ofSeconds(10))
            .doBeforeRetry(retrySignal -> logger.warn("🔄 Weapon 消費者正在嘗試重試 (第 {} 次), 原因: {}",
                    retrySignal.totalRetries() + 1, retrySignal.failure().getMessage()));

    /**
     * 啟動所有 reactive 消費者
     * 分批啟動以避免 RabbitMQ Channel 創建失敗
     */
    @PostConstruct
    public void startConsumers() {
        logger.info("🚀 啟動 Reactive Weapon Consumer...");

        // 第一批：讀取操作（立即啟動）
        startGetAllWeaponsConsumer();
        startGetWeaponByNameConsumer();
        startGetWeaponsByOwnerConsumer();

        // 第二批：寫入和刪除操作（延遲 500ms）
        Mono.delay(Duration.ofMillis(500))
                .doOnNext(tick -> logger.info("🔄 啟動 Weapon 寫入和刪除 Consumers..."))
                .subscribe(tick -> {
                    startSaveWeaponConsumer();
                    startDeleteWeaponConsumer();
                    startDeleteAllWeaponsConsumer();

                    // 第三批：檢查操作（再延遲 500ms）
                    Mono.delay(Duration.ofMillis(500))
                            .doOnNext(tick2 -> logger.info("🔄 啟動 Weapon 檢查 Consumers..."))
                            .subscribe(tick2 -> {
                                startCheckWeaponExistsConsumer();
                                logger.info("✅ Reactive Weapon Consumer 全部啟動完成");
                            });
                });
    }

    /**
     * Weapon Get-All 消費者
     */
    private void startGetAllWeaponsConsumer() {
        subscriptions.add(
                reactiveReceiver
                        .consumeManualAck(RabbitMQConfig.WEAPON_GET_ALL_QUEUE, new ConsumeOptions().qos(2))
                        .flatMap(this::handleGetAllWeapons, 2)
                        .doOnError(error -> logger.error("❌ Weapon Get-All 消費者發生錯誤: {}", error.getMessage()))
                        .retryWhen(defaultRetry)
                        .subscribe());

        logger.info("📡 啟動 Weapon Get-All Reactive Consumer (concurrency=2, prefetch=2)");
    }

    /**
     * Weapon Get-By-Name 消費者
     */
    private void startGetWeaponByNameConsumer() {
        subscriptions.add(
                reactiveReceiver
                        .consumeManualAck(RabbitMQConfig.WEAPON_GET_BY_NAME_QUEUE, new ConsumeOptions().qos(2))
                        .flatMap(this::handleGetWeaponByName, 2)
                        .doOnError(error -> logger.error("❌ Weapon Get-By-Name 消費者發生錯誤: {}", error.getMessage()))
                        .retryWhen(defaultRetry)
                        .subscribe());

        logger.info("📡 啟動 Weapon Get-By-Name Reactive Consumer (concurrency=2)");
    }

    /**
     * Weapon Get-By-Owner 消費者
     */
    private void startGetWeaponsByOwnerConsumer() {
        subscriptions.add(
                reactiveReceiver
                        .consumeManualAck(RabbitMQConfig.WEAPON_GET_BY_OWNER_QUEUE, new ConsumeOptions().qos(2))
                        .flatMap(this::handleGetWeaponsByOwner, 2)
                        .doOnError(error -> logger.error("❌ Weapon Get-By-Owner 消費者發生錯誤: {}", error.getMessage()))
                        .retryWhen(defaultRetry)
                        .subscribe());

        logger.info("📡 啟動 Weapon Get-By-Owner Reactive Consumer (concurrency=2)");
    }

    /**
     * Weapon Save 消費者
     */
    private void startSaveWeaponConsumer() {
        subscriptions.add(
                reactiveReceiver
                        .consumeManualAck(RabbitMQConfig.WEAPON_SAVE_QUEUE, new ConsumeOptions().qos(1))
                        .flatMap(this::handleSaveWeapon, 1)
                        .doOnError(error -> logger.error("❌ Weapon Save 消費者發生錯誤: {}", error.getMessage()))
                        .retryWhen(defaultRetry)
                        .subscribe());

        logger.info("📡 啟動 Weapon Save Reactive Consumer (concurrency=1)");
    }

    /**
     * Weapon Delete 消費者
     */
    private void startDeleteWeaponConsumer() {
        subscriptions.add(
                reactiveReceiver
                        .consumeManualAck(RabbitMQConfig.WEAPON_DELETE_QUEUE, new ConsumeOptions().qos(1))
                        .flatMap(this::handleDeleteWeapon, 1)
                        .doOnError(error -> logger.error("❌ Weapon Delete 消費者發生錯誤: {}", error.getMessage()))
                        .retryWhen(defaultRetry)
                        .subscribe());

        logger.info("📡 啟動 Weapon Delete Reactive Consumer (concurrency=1)");
    }

    /**
     * Weapon Delete-All 消費者
     */
    private void startDeleteAllWeaponsConsumer() {
        subscriptions.add(
                reactiveReceiver
                        .consumeManualAck(RabbitMQConfig.WEAPON_DELETE_ALL_QUEUE, new ConsumeOptions().qos(1))
                        .flatMap(this::handleDeleteAllWeapons, 1)
                        .doOnError(error -> logger.error("❌ Weapon Delete-All 消費者發生錯誤: {}", error.getMessage()))
                        .retryWhen(defaultRetry)
                        .subscribe());

        logger.info("📡 啟動 Weapon Delete-All Reactive Consumer (concurrency=1)");
    }

    /**
     * Weapon Exists 消費者
     */
    private void startCheckWeaponExistsConsumer() {
        subscriptions.add(
                reactiveReceiver
                        .consumeManualAck(RabbitMQConfig.WEAPON_EXISTS_QUEUE, new ConsumeOptions().qos(2))
                        .flatMap(this::handleCheckWeaponExists, 2)
                        .doOnError(error -> logger.error("❌ Weapon Exists 消費者發生錯誤: {}", error.getMessage()))
                        .retryWhen(defaultRetry)
                        .subscribe());

        logger.info("📡 啟動 Weapon Exists Reactive Consumer (concurrency=2)");
    }

    /**
     * 處理 Get-All Weapons 請求 - 完全 reactive
     */
    private Mono<Void> handleGetAllWeapons(AcknowledgableDelivery delivery) {
        return Mono.defer(() -> {
            try {
                String messageJson = new String(delivery.getBody());
                logger.info("🎯 收到 Weapon Get-All 請求: {}", messageJson);

                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                logger.info("📝 處理請求: requestId={}", requestId);

                return weaponService.getAllWeapons()
                        .collectList()
                        .flatMap(weaponList -> {
                            logger.info("✅ 查詢完成: 共 {} 個武器, requestId={}", weaponList.size(), requestId);
                            return asyncResultService.sendCompletedResultReactive(requestId, weaponList)
                                    .doOnSuccess(v -> {
                                        logger.info("🎉 Weapon Get-All 處理完成: requestId={}", requestId);
                                        delivery.ack();
                                    })
                                    .doOnError(error -> {
                                        logger.error("❌ Weapon Get-All 發送結果失敗: requestId={}, error={}", requestId,
                                                error.getMessage());
                                        delivery.nack(false);
                                    });
                        })
                        .onErrorResume(error -> {
                            logger.error("❌ Weapon Get-All 處理失敗: requestId={}, error={}", requestId,
                                    error.getMessage());
                            return asyncResultService
                                    .sendFailedResultReactive(requestId, "獲取武器列表失敗: " + error.getMessage())
                                    .doFinally(signalType -> delivery.nack(false));
                        });

            } catch (Exception e) {
                logger.error("❌ 無法解析消息: error={}", e.getMessage());
                delivery.nack(false);
                return Mono.empty();
            }
        });
    }

    /**
     * 處理 Get-By-Name Weapon 請求 - 完全 reactive
     */
    private Mono<Void> handleGetWeaponByName(AcknowledgableDelivery delivery) {
        return Mono.defer(() -> {
            try {
                String messageJson = new String(delivery.getBody());
                logger.info("🎯 收到 Weapon Get-By-Name 請求: {}", messageJson);

                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                String name = (String) message.getPayload();
                logger.info("📝 處理請求: name={}, requestId={}", name, requestId);

                return weaponService.getWeaponById(name)
                        .flatMap(weapon -> {
                            logger.info("✅ 查詢成功: name={}, requestId={}", name, requestId);
                            return asyncResultService.sendCompletedResultReactive(requestId, weapon)
                                    .doOnSuccess(v -> {
                                        logger.info("🎉 Weapon Get-By-Name 處理完成: requestId={}", requestId);
                                        delivery.ack();
                                    })
                                    .doOnError(error -> {
                                        logger.error("❌ Weapon Get-By-Name 發送結果失敗: requestId={}, error={}", requestId,
                                                error.getMessage());
                                        delivery.nack(false);
                                    });
                        })
                        .switchIfEmpty(
                                asyncResultService.sendFailedResultReactive(requestId, "武器不存在: " + name)
                                        .doFinally(signalType -> delivery.nack(false)))
                        .onErrorResume(error -> {
                            logger.error("❌ Weapon Get-By-Name 處理失敗: requestId={}, error={}", requestId,
                                    error.getMessage());
                            return asyncResultService
                                    .sendFailedResultReactive(requestId, "獲取武器失敗: " + error.getMessage())
                                    .doFinally(signalType -> delivery.nack(false));
                        });

            } catch (Exception e) {
                logger.error("❌ 無法解析消息: error={}", e.getMessage());
                delivery.nack(false);
                return Mono.empty();
            }
        });
    }

    /**
     * 處理 Get-By-Owner Weapons 請求 - 完全 reactive
     */
    private Mono<Void> handleGetWeaponsByOwner(AcknowledgableDelivery delivery) {
        return Mono.defer(() -> {
            try {
                String messageJson = new String(delivery.getBody());
                logger.info("🎯 收到 Weapon Get-By-Owner 請求: {}", messageJson);

                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                String owner = (String) message.getPayload();
                logger.info("📝 處理請求: owner={}, requestId={}", owner, requestId);

                return weaponService.getWeaponsByOwner(owner)
                        .collectList()
                        .flatMap(weaponList -> {
                            logger.info("✅ 查詢完成: owner={}, 共 {} 個武器, requestId={}", owner, weaponList.size(),
                                    requestId);
                            return asyncResultService.sendCompletedResultReactive(requestId, weaponList)
                                    .doOnSuccess(v -> {
                                        logger.info("🎉 Weapon Get-By-Owner 處理完成: requestId={}", requestId);
                                        delivery.ack();
                                    })
                                    .doOnError(error -> {
                                        logger.error("❌ Weapon Get-By-Owner 發送結果失敗: requestId={}, error={}", requestId,
                                                error.getMessage());
                                        delivery.nack(false);
                                    });
                        })
                        .onErrorResume(error -> {
                            logger.error("❌ Weapon Get-By-Owner 處理失敗: requestId={}, error={}", requestId,
                                    error.getMessage());
                            return asyncResultService
                                    .sendFailedResultReactive(requestId, "獲取武器列表失敗: " + error.getMessage())
                                    .doFinally(signalType -> delivery.nack(false));
                        });

            } catch (Exception e) {
                logger.error("❌ 無法解析消息: error={}", e.getMessage());
                delivery.nack(false);
                return Mono.empty();
            }
        });
    }

    /**
     * 處理 Save Weapon 請求 - 完全 reactive
     */
    private Mono<Void> handleSaveWeapon(AcknowledgableDelivery delivery) {
        return Mono.defer(() -> {
            try {
                String messageJson = new String(delivery.getBody());
                logger.info("🎯 收到 Weapon Save 請求: {}", messageJson);

                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                Weapon weapon = objectMapper.convertValue(message.getPayload(), Weapon.class);
                logger.info("📝 處理請求: weapon={}, requestId={}", weapon.getName(), requestId);

                return weaponService.saveWeapon(weapon)
                        .flatMap(savedWeapon -> {
                            logger.info("✅ 保存成功: weapon={}, requestId={}", savedWeapon.getName(), requestId);
                            return asyncResultService.sendCompletedResultReactive(requestId, savedWeapon)
                                    .doOnSuccess(v -> {
                                        logger.info("🎉 Weapon Save 處理完成: requestId={}", requestId);
                                        delivery.ack();
                                    })
                                    .doOnError(error -> {
                                        logger.error("❌ Weapon Save 發送結果失敗: requestId={}, error={}", requestId,
                                                error.getMessage());
                                        delivery.nack(false);
                                    });
                        })
                        .onErrorResume(error -> {
                            logger.error("❌ Weapon Save 處理失敗: requestId={}, error={}", requestId, error.getMessage());
                            return asyncResultService
                                    .sendFailedResultReactive(requestId, "保存武器失敗: " + error.getMessage())
                                    .doFinally(signalType -> delivery.nack(false));
                        });

            } catch (Exception e) {
                logger.error("❌ 無法解析消息: error={}", e.getMessage());
                delivery.nack(false);
                return Mono.empty();
            }
        });
    }

    /**
     * 處理 Check Weapon Exists 請求 - 完全 reactive
     */
    private Mono<Void> handleCheckWeaponExists(AcknowledgableDelivery delivery) {
        return Mono.defer(() -> {
            try {
                String messageJson = new String(delivery.getBody());
                logger.info("🎯 收到 Weapon Exists 請求: {}", messageJson);

                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                String name = (String) message.getPayload();
                logger.info("📝 處理請求: name={}, requestId={}", name, requestId);

                return weaponService.weaponExists(name)
                        .flatMap(exists -> {
                            logger.info("✅ 檢查完成: name={}, exists={}, requestId={}", name, exists, requestId);
                            return asyncResultService.sendCompletedResultReactive(requestId, exists)
                                    .doOnSuccess(v -> {
                                        logger.info("🎉 Weapon Exists 處理完成: requestId={}", requestId);
                                        try {
                                            delivery.ack();
                                        } catch (Exception e) {
                                            logger.error("❌ ACK 失敗: requestId={}", requestId, e);
                                        }
                                    })
                                    .doOnError(error -> {
                                        logger.error("❌ Weapon Exists 發送結果失敗: requestId={}, error={}", requestId,
                                                error.getMessage());
                                        try {
                                            delivery.nack(false);
                                        } catch (Exception e) {
                                            logger.error("❌ NACK 失敗: requestId={}", requestId, e);
                                        }
                                    });
                        })
                        .onErrorResume(error -> {
                            logger.error("❌ Weapon Exists 處理失敗: requestId={}, error={}", requestId, error.getMessage());
                            return asyncResultService
                                    .sendFailedResultReactive(requestId, "檢查武器存在失敗: " + error.getMessage())
                                    .doFinally(signalType -> {
                                        try {
                                            delivery.nack(false);
                                        } catch (Exception e) {
                                            logger.error("❌ NACK 失敗: requestId={}", requestId, e);
                                        }
                                    });
                        });

            } catch (Exception e) {
                logger.error("❌ 無法解析消息: error={}", e.getMessage());
                try {
                    delivery.nack(false);
                } catch (Exception ne) {
                    logger.error("❌ NACK 失敗: error={}", ne.getMessage(), ne);
                }
                return Mono.empty();
            }
        });
    }

    /**
     * 處理 Delete Weapon 請求 - 完全 reactive
     */
    private Mono<Void> handleDeleteWeapon(AcknowledgableDelivery delivery) {
        return Mono.defer(() -> {
            try {
                String messageJson = new String(delivery.getBody());
                logger.info("🎯 收到 Weapon Delete 請求: {}", messageJson);

                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                String weaponId = (String) message.getPayload();
                logger.info("📝 處理請求: weaponId={}, requestId={}", weaponId, requestId);

                return weaponService.deleteWeapon(weaponId)
                        .then(Mono.defer(() -> {
                            logger.info("✅ 刪除成功: weaponId={}, requestId={}", weaponId, requestId);
                            return asyncResultService.sendCompletedResultReactive(requestId, true)
                                    .doOnSuccess(v -> {
                                        logger.info("🎉 Weapon Delete 處理完成: requestId={}", requestId);
                                        delivery.ack();
                                    })
                                    .doOnError(error -> {
                                        logger.error("❌ Weapon Delete 發送結果失敗: requestId={}, error={}", requestId,
                                                error.getMessage());
                                        delivery.nack(false);
                                    });
                        }))
                        .onErrorResume(error -> {
                            logger.error("❌ Weapon Delete 處理失敗: requestId={}, error={}", requestId, error.getMessage());
                            return asyncResultService
                                    .sendFailedResultReactive(requestId, "刪除武器失敗: " + error.getMessage())
                                    .doFinally(signalType -> delivery.nack(false));
                        });

            } catch (Exception e) {
                logger.error("❌ 無法解析消息: error={}", e.getMessage());
                delivery.nack(false);
                return Mono.empty();
            }
        });
    }

    /**
     * 處理 Delete-All Weapons 請求 - 完全 reactive
     */
    private Mono<Void> handleDeleteAllWeapons(AcknowledgableDelivery delivery) {
        return Mono.defer(() -> {
            try {
                String messageJson = new String(delivery.getBody());
                logger.info("🎯 收到 Weapon Delete-All 請求: {}", messageJson);

                AsyncMessageDTO message = objectMapper.readValue(messageJson, AsyncMessageDTO.class);
                String requestId = message.getRequestId();
                logger.info("📝 處理請求: requestId={}", requestId);

                return weaponService.deleteAllWeapons()
                        .then(Mono.defer(() -> {
                            logger.info("✅ 批量刪除完成, requestId={}", requestId);
                            return asyncResultService.sendCompletedResultReactive(requestId, "所有武器已刪除")
                                    .doOnSuccess(v -> {
                                        logger.info("🎉 Weapon Delete-All 處理完成: requestId={}", requestId);
                                        delivery.ack();
                                    })
                                    .doOnError(error -> {
                                        logger.error("❌ Weapon Delete-All 發送結果失敗: requestId={}, error={}", requestId,
                                                error.getMessage());
                                        delivery.nack(false);
                                    });
                        }))
                        .onErrorResume(error -> {
                            logger.error("❌ Weapon Delete-All 處理失敗: requestId={}, error={}", requestId,
                                    error.getMessage());
                            return asyncResultService
                                    .sendFailedResultReactive(requestId, "批量刪除武器失敗: " + error.getMessage())
                                    .doFinally(signalType -> delivery.nack(false));
                        });

            } catch (Exception e) {
                logger.error("❌ 無法解析消息: error={}", e.getMessage());
                delivery.nack(false);
                return Mono.empty();
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        logger.info("🛑 關閉 Reactive Weapon Consumer...");
        subscriptions.dispose();
        logger.info("✅ 所有訂閱已解除 (Remaining: {})", subscriptions.isDisposed());
    }
}
