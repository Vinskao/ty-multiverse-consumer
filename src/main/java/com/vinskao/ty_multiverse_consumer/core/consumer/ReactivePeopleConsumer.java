package com.vinskao.ty_multiverse_consumer.core.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinskao.ty_multiverse_consumer.config.RabbitMQConfig;
import com.vinskao.ty_multiverse_consumer.core.dto.AsyncMessageDTO;
import com.vinskao.ty_multiverse_consumer.core.service.AsyncResultService;
import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.People;
import com.vinskao.ty_multiverse_consumer.module.people.service.PeopleService;
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
 * 完全 Reactive People Consumer
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
public class ReactivePeopleConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ReactivePeopleConsumer.class);

    @Autowired
    private Receiver reactiveReceiver;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PeopleService peopleService;

    @Autowired
    private AsyncResultService asyncResultService;

    /**
     * 啟動所有 reactive 消費者
     */
    @PostConstruct
    public void startConsumers() {
        logger.info("🚀 啟動 Reactive People Consumer...");
        
        // 啟動各個隊列的消費者
        startGetAllPeopleConsumer();
        startGetPeopleByNameConsumer();
        startDeleteAllPeopleConsumer();
        
        logger.info("✅ Reactive People Consumer 啟動完成");
    }

    /**
     * People Get-All 消費者
     * 
     * 使用 flatMap(concurrency=2) 控制並發，與 DB 連線池協調
     */
    private void startGetAllPeopleConsumer() {
        reactiveReceiver
            .consumeManualAck(RabbitMQConfig.PEOPLE_GET_ALL_QUEUE, new ConsumeOptions().qos(2))
            .flatMap(this::handleGetAllPeople, 2) // 並發控制：最多2個並發處理
            .doOnError(error -> logger.error("❌ People Get-All 消費者發生錯誤", error))
            .retry() // 自動重試
            .subscribe();
        
        logger.info("📡 啟動 People Get-All Reactive Consumer (concurrency=2, prefetch=2)");
    }

    /**
     * People Get-By-Name 消費者
     */
    private void startGetPeopleByNameConsumer() {
        reactiveReceiver
            .consumeManualAck(RabbitMQConfig.PEOPLE_GET_BY_NAME_QUEUE, new ConsumeOptions().qos(2))
            .flatMap(this::handleGetPeopleByName, 2)
            .doOnError(error -> logger.error("❌ People Get-By-Name 消費者發生錯誤", error))
            .retry()
            .subscribe();
        
        logger.info("📡 啟動 People Get-By-Name Reactive Consumer (concurrency=2)");
    }

    /**
     * People Delete-All 消費者
     */
    private void startDeleteAllPeopleConsumer() {
        reactiveReceiver
            .consumeManualAck(RabbitMQConfig.PEOPLE_DELETE_ALL_QUEUE, new ConsumeOptions().qos(1))
            .flatMap(this::handleDeleteAllPeople, 1) // 刪除操作序列化處理
            .doOnError(error -> logger.error("❌ People Delete-All 消費者發生錯誤", error))
            .retry()
            .subscribe();
        
        logger.info("📡 啟動 People Delete-All Reactive Consumer (concurrency=1)");
    }

    /**
     * 處理 Get-All People 請求 - 完全 reactive
     */
    private Mono<Void> handleGetAllPeople(AcknowledgableDelivery delivery) {
        String messageJson = new String(delivery.getBody());
        
        return Mono.fromCallable(() -> {
                logger.info("🎯 收到 People Get-All 請求: {}", messageJson);
                return objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            })
            .flatMap(message -> {
                String requestId = message.getRequestId();
                logger.info("📝 處理請求: requestId={}", requestId);
                
                // 完全 reactive 的資料庫查詢
                return peopleService.getAllPeopleOptimized()
                    .collectList()
                    .flatMap(peopleList -> {
                        logger.info("✅ 查詢完成: 共 {} 個角色, requestId={}", peopleList.size(), requestId);
                        
                        // 發送成功結果 - 這裡需要確保 asyncResultService 也是 reactive 的
                        return asyncResultService.sendCompletedResultReactive(requestId, peopleList);
                    })
                    .doOnSuccess(v -> {
                        logger.info("🎉 People Get-All 處理完成: requestId={}", requestId);
                        delivery.ack(); // 手動 ACK
                    })
                    .doOnError(error -> {
                        logger.error("❌ People Get-All 處理失敗: requestId={}, error={}", requestId, error.getMessage());
                        
                        // 發送錯誤結果
                        asyncResultService.sendFailedResultReactive(requestId, "獲取角色列表失敗: " + error.getMessage())
                            .doFinally(signalType -> delivery.nack(false)) // 手動 NACK，不重新入隊
                            .subscribe();
                    });
            })
            .onErrorResume(parseError -> {
                logger.error("❌ 無法解析消息: {}, error={}", messageJson, parseError.getMessage());
                delivery.nack(false); // 解析錯誤，不重新入隊
                return Mono.empty();
            })
            .then();
    }

    /**
     * 處理 Get-By-Name People 請求 - 完全 reactive
     */
    private Mono<Void> handleGetPeopleByName(AcknowledgableDelivery delivery) {
        String messageJson = new String(delivery.getBody());
        
        return Mono.fromCallable(() -> {
                logger.info("🎯 收到 People Get-By-Name 請求: {}", messageJson);
                return objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            })
            .flatMap(message -> {
                String requestId = message.getRequestId();
                String name = (String) message.getPayload();
                logger.info("📝 處理請求: name={}, requestId={}", name, requestId);
                
                return peopleService.getPeopleByName(name)
                    .flatMap(people -> {
                        logger.info("✅ 查詢成功: name={}, requestId={}", name, requestId);
                        return asyncResultService.sendCompletedResultReactive(requestId, people);
                    })
                    .switchIfEmpty(
                        Mono.defer(() -> {
                            logger.warn("⚠️ 角色不存在: name={}, requestId={}", name, requestId);
                            return asyncResultService.sendFailedResultReactive(requestId, "角色不存在: " + name);
                        })
                    )
                    .doOnSuccess(v -> {
                        logger.info("🎉 People Get-By-Name 處理完成: requestId={}", requestId);
                        delivery.ack();
                    })
                    .doOnError(error -> {
                        logger.error("❌ People Get-By-Name 處理失敗: requestId={}, error={}", requestId, error.getMessage());
                        
                        asyncResultService.sendFailedResultReactive(requestId, "獲取角色失敗: " + error.getMessage())
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
     * 處理 Delete-All People 請求 - 完全 reactive
     */
    private Mono<Void> handleDeleteAllPeople(AcknowledgableDelivery delivery) {
        String messageJson = new String(delivery.getBody());
        
        return Mono.fromCallable(() -> {
                logger.info("🎯 收到 People Delete-All 請求: {}", messageJson);
                return objectMapper.readValue(messageJson, AsyncMessageDTO.class);
            })
            .flatMap(message -> {
                String requestId = message.getRequestId();
                logger.info("📝 處理請求: requestId={}", requestId);
                
                return peopleService.deleteAllPeopleReactive()
                    .flatMap(deletedCount -> {
                        logger.info("✅ 刪除完成: 共刪除 {} 個角色, requestId={}", deletedCount, requestId);
                        return asyncResultService.sendCompletedResultReactive(requestId, null);
                    })
                    .doOnSuccess(v -> {
                        logger.info("🎉 People Delete-All 處理完成: requestId={}", requestId);
                        delivery.ack();
                    })
                    .doOnError(error -> {
                        logger.error("❌ People Delete-All 處理失敗: requestId={}, error={}", requestId, error.getMessage());
                        
                        asyncResultService.sendFailedResultReactive(requestId, "刪除所有角色失敗: " + error.getMessage())
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
        logger.info("🛑 關閉 Reactive People Consumer...");
        // Receiver 會自動關閉連接
    }
}
