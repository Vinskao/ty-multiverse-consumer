package com.vinskao.ty_multiverse_consumer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tw.com.ty.common.resilience.annotation.Retryable;

/**
 * 重試功能示例服務
 *
 * 演示如何使用 @Retryable 註解進行自動重試
 */
@Service
public class ExampleRetryService {

    private static final Logger logger = LoggerFactory.getLogger(ExampleRetryService.class);
    private int callCount = 0;

    /**
     * 數據庫操作重試示例
     * 在遇到連接異常時自動重試
     */
    @Retryable(
        value = {org.springframework.dao.DataAccessException.class, java.sql.SQLTransientConnectionException.class},
        maxAttempts = 5,
        initialDelay = 2000,
        maxDelay = 10000,
        multiplier = 2.0
    )
    public String performDatabaseOperation(String data) {
        callCount++;
        logger.info("🔄 Executing database operation (attempt {}) for data: {}", callCount, data);

        // 模擬前兩次失敗，第三次成功
        if (callCount < 3) {
            logger.warn("❌ Database operation failed (simulated), attempt: {}", callCount);
            throw new org.springframework.dao.DataAccessException("Simulated database connection error") {};
        }

        logger.info("✅ Database operation succeeded on attempt {}", callCount);
        callCount = 0; // 重置計數器
        return "Processed: " + data;
    }

    /**
     * 網路調用重試示例
     * 在遇到連接超時時自動重試
     */
    @Retryable(
        value = {java.net.ConnectException.class, java.net.SocketTimeoutException.class},
        maxAttempts = 3,
        initialDelay = 1000,
        maxDelay = 5000,
        multiplier = 1.5
    )
    public String performNetworkCall(String url) throws java.net.ConnectException {
        logger.info("🌐 Making network call to: {}", url);

        // 模擬網路連接失敗
        if (Math.random() < 0.7) { // 70% 失敗率
            logger.warn("❌ Network call failed (simulated)");
            throw new java.net.ConnectException("Simulated network connection error");
        }

        logger.info("✅ Network call succeeded");
        return "Response from: " + url;
    }

    /**
     * 通用業務操作重試示例
     * 使用預設重試配置
     */
    @Retryable(maxAttempts = 3)
    public String performBusinessOperation(String operation) {
        logger.info("💼 Performing business operation: {}", operation);

        // 模擬隨機失敗
        if (Math.random() < 0.5) {
            logger.warn("❌ Business operation failed (simulated)");
            throw new RuntimeException("Simulated business error");
        }

        logger.info("✅ Business operation succeeded");
        return "Result of: " + operation;
    }
}
