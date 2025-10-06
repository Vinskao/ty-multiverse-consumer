package com.vinskao.ty_multiverse_consumer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;
import java.time.Duration;

/**
 * 數據庫連接服務
 * 
 * 提供智能的數據庫連接等待和重試機制
 */
@Service
public class DatabaseConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionService.class);

    @Autowired
    private ConnectionFactory connectionFactory;

    /**
     * 等待數據庫連接可用 (R2DBC Reactive)
     * 
     * @param maxWaitSeconds 最大等待秒數
     * @return Mono 包裝的連接狀態
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 20,
        backoff = @Backoff(delay = 5000, multiplier = 1.5, maxDelay = 60000)
    )
    public Mono<Boolean> waitForConnection(int maxWaitSeconds) {
        long startTime = System.currentTimeMillis();
        
        return Mono.from(connectionFactory.create())
            .flatMap(connection -> {
                logger.info("成功獲取 R2DBC 數據庫連接，等待時間: {} 秒", 
                           (System.currentTimeMillis() - startTime) / 1000);
                return Mono.fromRunnable(() -> connection.close())
                    .then(Mono.just(true));
            })
            .timeout(Duration.ofSeconds(maxWaitSeconds))
            .doOnError(error -> logger.error("等待數據庫連接失敗: {}", error.getMessage()))
            .onErrorReturn(false);
    }

    /**
     * 檢查數據庫連接狀態 (R2DBC Reactive)
     * 
     * @return Mono 包裝的連接狀態
     */
    public Mono<Boolean> isConnectionAvailable() {
        return Mono.from(connectionFactory.create())
            .flatMap(connection -> Mono.fromRunnable(() -> connection.close())
                .then(Mono.just(true)))
            .timeout(Duration.ofSeconds(5))
            .doOnError(error -> logger.warn("R2DBC 數據庫連接檢查失敗: {}", error.getMessage()))
            .onErrorReturn(false);
    }

    /**
     * 強制等待連接可用 (R2DBC Reactive)
     * 
     * @return Mono 包裝的連接狀態
     */
    public Mono<Boolean> forceWaitForConnection() {
        logger.info("開始強制等待 R2DBC 數據庫連接...");
        return waitForConnection(300); // 等待5分鐘
    }
}

